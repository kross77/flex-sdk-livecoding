package codeOrchestra;

import codeOrchestra.digest.DigestManager;
import codeOrchestra.tree.*;
import codeOrchestra.util.StringUtils;
import flex2.compiler.CompilationUnit;
import flex2.tools.Fcsh;
import macromedia.asc.parser.*;
import macromedia.asc.util.Context;
import macromedia.asc.util.ObjectList;

import java.util.*;

/**
 * @author Anton.I.Neverov
 */
public class LCBaseExtension extends AbstractTreeModificationExtension {

    private Map<String, String> modelDependenciesUnits = new HashMap<String, String>();
    private boolean liveCodingStarterAdded;

    @Override
    protected void performModifications(CompilationUnit unit) {
        ClassDefinitionNode classDefinitionNode = TreeNavigator.getClassDefinition(unit);
        if (classDefinitionNode == null) {
            return;
        }

        if (Fcsh.livecodingBaseModeSecondPass) {
            loadSyntaxTrees();
        } else {
            DigestManager.getInstance().addToDigestUnresolved(classDefinitionNode);
            saveSyntaxTree(unit);
            return;
        }

        String packageName = classDefinitionNode.pkgdef.name.id.pkg_part;
        String className = classDefinitionNode.name.name;

        if (!modelDependenciesUnits.keySet().contains(packageName) && !ProvidedPackages.isProvidedPackage(packageName)) {
            String mdClassName = addModelDependenciesUnit(packageName, classDefinitionNode.cx);
            modelDependenciesUnits.put(packageName, mdClassName);
        }

        if (LiveCodingUtil.canBeUsedForLiveCoding(classDefinitionNode)) {
            classDefinitionNode.attrs.items.add(TreeUtil.createDynamicModifier());

            TreeUtil.addImport(unit, "codeOrchestra.actionScript.liveCoding.util", "LiveCodeRegistry");
            TreeUtil.addImport(unit, "codeOrchestra.actionScript.liveCoding.util", "MethodUpdateEvent");

            for (FunctionDefinitionNode methodDefinition : TreeNavigator.getMethodDefinitions(classDefinitionNode)) {
                if (LiveCodingUtil.canBeUsedForLiveCoding(methodDefinition)) {
                    extractMethodToLiveCodingClass(methodDefinition, classDefinitionNode);
                }
            }

            // Extract all internal classes
            List<String> internalClassesNames = new ArrayList<String>();
            ProgramNode syntaxTree = (ProgramNode) unit.getSyntaxTree();
            for (ClassDefinitionNode internalClass : TreeNavigator.getInternalClassDefinitions(syntaxTree)) {
                internalClassesNames.add(internalClass.name.name);

                // Detach
                syntaxTree.statements.items.remove(internalClass);

                // Make public
                internalClass.attrs = new AttributeListNode(TreeUtil.createPublicModifier(), -1);

                // Add as a separate unit
                TreeUtil.createUnitFromInternalClass(internalClass, packageName, classDefinitionNode.cx, TreeNavigator.getImports(syntaxTree), unit.inheritance);
            }

            /*
               private static var __modelDependencies : ModelDependencies_com_example  = new ModelDependencies_com_example() ;
            */
            AttributeListNode attrs = new AttributeListNode(TreeUtil.createPrivateModifier(), -1);
            attrs.items.add(TreeUtil.createStaticModifier());
            TypedIdentifierNode variable = new TypedIdentifierNode(
                    new QualifiedIdentifierNode(attrs, "__modelDependencies", -1),
                    new TypeExpressionNode(TreeUtil.createIdentifier(modelDependenciesUnits.get(packageName)) ,true, false),
                    -1
            );
            CallExpressionNode selector = new CallExpressionNode(new IdentifierNode(modelDependenciesUnits.get(packageName), -1), null);
            selector.is_new = true;
            MemberExpressionNode initializer = new MemberExpressionNode(null, selector, -1);
            ListNode listNode = new ListNode(
                    null,
                    new VariableBindingNode(classDefinitionNode.pkgdef, attrs, Tokens.VAR_TOKEN, variable, initializer),
                    -1
            );
            classDefinitionNode.statements.items.add(new VariableDefinitionNode(classDefinitionNode.pkgdef, attrs, Tokens.VAR_TOKEN, listNode, -1));

            // COLT-67
            for (String ownLiveMethodClass : projectNavigator.getLiveCodingClassNames(packageName, className)) {
                classDefinitionNode.statements.items.add(new ExpressionStatementNode(new ListNode(null, TreeUtil.createIdentifier(ownLiveMethodClass, "prototype"), -1)));
            }
            for (String internalClassName : internalClassesNames) {
                classDefinitionNode.statements.items.add(new ExpressionStatementNode(new ListNode(null, TreeUtil.createIdentifier(internalClassName, "prototype"), -1)));
            }
        }
    }

    private void extractMethodToLiveCodingClass(FunctionDefinitionNode functionDefinitionNode, ClassDefinitionNode classDefinitionNode) {
        ObjectList<Node> oldBody = functionDefinitionNode.fexpr.body.items;
        functionDefinitionNode.fexpr.body.items = new ObjectList<Node>();

        fillStubMethodBody(functionDefinitionNode, classDefinitionNode.name.name);
        makePrivateMembersPublic(classDefinitionNode);
        addLiveCodingClass(classDefinitionNode.name.name, functionDefinitionNode, oldBody, false);

        if (LiveCodingUtil.isLiveCodeUpdateListener(functionDefinitionNode)) {
            addListener(functionDefinitionNode, classDefinitionNode);
        }
    }

    private void makePrivateMembersPublic(ClassDefinitionNode classDefinitionNode) {
        // Fields
        for (VariableDefinitionNode variableDefinitionNode : TreeNavigator.getFieldDefinitions(classDefinitionNode)) {
            ListNode listNode = variableDefinitionNode.list;
            if (listNode != null) {
                for (Node item : listNode.items) {
                    if (item instanceof VariableBindingNode) {
                        VariableBindingNode variableBindingNode = (VariableBindingNode) item;
                        AttributeListNode attributeListNode = variableBindingNode.attrs;
                        TreeUtil.makePublic(attributeListNode);
                    }
                }
            }
        }

        // Methods
        for (FunctionDefinitionNode functionDefinitionNode : TreeNavigator.getMethodDefinitions(classDefinitionNode)) {
            TreeUtil.makePublic(functionDefinitionNode.attrs);
        }
    }

    private void fillStubMethodBody(FunctionDefinitionNode functionDefinitionNode, String className) {
        boolean staticMethod = TreeNavigator.isStaticMethod(functionDefinitionNode);
        boolean isVoid = functionDefinitionNode.fexpr.signature.result == null;
        ObjectList<Node> newBody = functionDefinitionNode.fexpr.body.items;
        PackageDefinitionNode pkgdef = functionDefinitionNode.pkgdef;

        /*
            var method : Class  = LiveCodeRegistry.getInstance().getMethod("com.example.Main.foo");
            or
            var method : *  = LiveCodeRegistry.getInstance().getMethod("com.example.Main.static.getColor");
         */

        TypeExpressionNode methodType = staticMethod ? null : new TypeExpressionNode(
                TreeUtil.createIdentifier("Class"),
                true,
                false
        );
        TypedIdentifierNode variable = new TypedIdentifierNode(
                new QualifiedIdentifierNode(null, "method", -1),
                methodType,
                -1
        );
        String remoteMethodName = pkgdef.name.id.pkg_part + "." + className + "." + (staticMethod ? "static." : "") + functionDefinitionNode.name.identifier.name;
        MemberExpressionNode initializer = new MemberExpressionNode(
                TreeUtil.createCall("LiveCodeRegistry", "getInstance", null),
                new CallExpressionNode(
                        new IdentifierNode("getMethod", -1),
                        new ArgumentListNode(new LiteralStringNode(remoteMethodName),-1)
                ),
                -1
        );
        ListNode listNode = new ListNode(
                null,
                new VariableBindingNode(pkgdef, null, Tokens.VAR_TOKEN, variable, initializer),
                -1
        );
        newBody.add(new VariableDefinitionNode(pkgdef, null, Tokens.VAR_TOKEN, listNode, -1));

        /*
            TODO: Test block
            new LiveMethod_com_example_Main_foo();
         */
//        ArgumentListNode args = null;
//        if (!staticMethod) {
//            args = new ArgumentListNode(new ThisExpressionNode(),-1);
//        }
//        CallExpressionNode testCall = new CallExpressionNode(new IdentifierNode(LiveCodingUtil.constructLiveCodingClassName(functionDefinitionNode, className), -1), args);
//        testCall.is_new = true;
//        newBody.add(new ExpressionStatementNode(new ListNode(null, new MemberExpressionNode(null, testCall, -1), -1)));

        /*
            [return] (new method(this)).run();
            or
            [return] method.run();
         */

        // COLT-57
        ArgumentListNode argumentListNode = null;
        ParameterListNode parameterListNode = functionDefinitionNode.fexpr.signature.parameter;
        if (parameterListNode != null && parameterListNode.items.size() > 0) {
            argumentListNode = new ArgumentListNode(TreeUtil.createIdentifier(parameterListNode.items.get(0).identifier.name), -1);
            if (parameterListNode.items.size() > 1) {
                for (int i = 1; i < parameterListNode.items.size(); i++) {
                    argumentListNode.items.add(TreeUtil.createIdentifier(parameterListNode.items.get(i).identifier.name));
                }
            }
        }
        ListNode expr;
        if (!staticMethod) {
            CallExpressionNode callExpressionNode = new CallExpressionNode(
                    new IdentifierNode("method", -1),
                    new ArgumentListNode(new ThisExpressionNode(),-1)
            );
            callExpressionNode.is_new = true;
            ListNode base = new ListNode(null, new MemberExpressionNode(null, callExpressionNode, -1), -1);
            CallExpressionNode selector = new CallExpressionNode(new IdentifierNode("run", -1), argumentListNode);
            expr = new ListNode(null, new MemberExpressionNode(base, selector, -1), -1);
        } else {
            expr = new ListNode(null, TreeUtil.createCall("method", "run", argumentListNode), -1);
        }
        if (isVoid) {
            newBody.add(new ExpressionStatementNode(expr));
        } else {
            newBody.add(new ReturnStatementNode(expr));
        }

        /*
            return;
         */
        newBody.add(new ReturnStatementNode(null));
    }

    private void addListener(FunctionDefinitionNode functionDefinitionNode, ClassDefinitionNode classDefinitionNode) {
        /*
            public function foo_codeUpdateListener4703382380319456072 ( e : MethodUpdateEvent ) : void {
                if ( e.classFqn == "com.example.Main" ) {
                    foo();
                }
            }
         */

        String classFqn = StringUtils.longNameFromNamespaceAndShortName(functionDefinitionNode.pkgdef.name.id.pkg_part, classDefinitionNode.name.name);
        String methodNameToFilter = null;
        boolean weak = true;
        int priority = 0;

        MetaDataNode annotation = LiveCodingUtil.getAnnotation(functionDefinitionNode, "LiveCodeUpdateListener");
        if (annotation != null) {
            String annotationClassFqn = annotation.getValue("classFqn");
            if (!StringUtils.isEmpty(annotationClassFqn)) {
                classFqn = annotationClassFqn;
            }

            String annotationMethod = annotation.getValue("method");
            if (!StringUtils.isEmpty(annotationMethod)) {
                methodNameToFilter = annotationMethod;
            }

            String annotationWeak = annotation.getValue("weak");
            if (!StringUtils.isEmpty(annotationWeak)) {
                weak = Boolean.parseBoolean(annotationWeak);
            }

            String annotationPriority = annotation.getValue("priority");
            if (!StringUtils.isEmpty(annotationPriority)) {
                priority = Integer.parseInt(annotationPriority);
            }
        }

        boolean staticMethod = TreeNavigator.isStaticMethod(functionDefinitionNode);
        String id = UUID.randomUUID().toString(); // TODO: Just any unique id?
        String listenerName = functionDefinitionNode.name.identifier.name + "_codeUpdateListener" + id;
        MethodCONode listener = new MethodCONode(listenerName, null, classDefinitionNode.cx);
        listener.addParameter("e", "MethodUpdateEvent");

        Node firstStatement = null;
        MemberExpressionNode methodCall = TreeUtil.createCall(null, functionDefinitionNode.name.identifier.name, null);
        if ("*".equals(classFqn)) {
            firstStatement = new ExpressionStatementNode(new ListNode(null, methodCall, -1));
        } else {
            ListNode condition = new ListNode(
                    null,
                    new BinaryExpressionNode(
                            Tokens.EQUALS_TOKEN,
                            TreeUtil.createIdentifier("e", "classFqn"),
                            new LiteralStringNode(classFqn)
                    ),
                    -1
            );
            StatementListNode thenactions = new StatementListNode(new ExpressionStatementNode(new ListNode(
                    null,
                    methodCall,
                    -1
            )));
            firstStatement = new IfStatementNode(condition, thenactions, null);
        }

        if (!StringUtils.isEmpty(methodNameToFilter)) {
            ListNode condition = new ListNode(
                    null,
                    new BinaryExpressionNode(
                            Tokens.EQUALS_TOKEN,
                            TreeUtil.createIdentifier("e", "methodName"),
                            new LiteralStringNode(methodNameToFilter)
                    ),
                    -1
            );
            StatementListNode thenactions = new StatementListNode(new ExpressionStatementNode(new ListNode(
                    null,
                    firstStatement,
                    -1
            )));
            firstStatement = new IfStatementNode(condition, thenactions, null);
        }

        listener.statements.add(firstStatement);
        listener.statements.add(new ReturnStatementNode(null));
        listener.isStatic = staticMethod;
        classDefinitionNode.statements.items.add(listener.getFunctionDefinitionNode());

        /*
            LiveCodeRegistry.getInstance().addEventListener(MethodUpdateEvent.METHOD_UPDATE, this.foo_codeUpdateListener4703382380319456072, false, 0, true);
            or
            LiveCodeRegistry.getInstance().addEventListener(MethodUpdateEvent.METHOD_UPDATE, Main.getColor_codeUpdateListener2123954341648648741, false, 0, true);
         */
        FunctionDefinitionNode constructorDefinition = TreeNavigator.getConstructorDefinition(classDefinitionNode);
        ArgumentListNode args = new ArgumentListNode(TreeUtil.createIdentifier("MethodUpdateEvent", "METHOD_UPDATE"), -1);
        MemberExpressionNode qListenerName = staticMethod ? TreeUtil.createIdentifier(classDefinitionNode.name.name, listenerName) : TreeUtil.createThisIdentifier(listenerName);
        args.items.add(qListenerName);
        args.items.add(new LiteralBooleanNode(false));
        args.items.add(new LiteralNumberNode(String.valueOf(priority)));
        args.items.add(new LiteralBooleanNode(weak));
        CallExpressionNode selector = new CallExpressionNode(new IdentifierNode("addEventListener", -1), args);
        MemberExpressionNode item = new MemberExpressionNode(TreeUtil.createCall("LiveCodeRegistry", "getInstance", null), selector, -1);
        ExpressionStatementNode listenerAddExpressionStatement = new ExpressionStatementNode(new ListNode(null, item, -1));
        ObjectList<Node> constructorBody = constructorDefinition.fexpr.body.items;
        constructorBody.add(constructorBody.size() - 1, listenerAddExpressionStatement);
    }

    private String addModelDependenciesUnit(String packageName, Context cx) {
        String className = "ModelDependencies_" + packageName.replaceAll("\\.", "_");
        ClassCONode classCONode = new ClassCONode(packageName, className, cx);

        MethodCONode constructor = new MethodCONode(className, null, cx);

        for (String name : projectNavigator.getClassNames(packageName)) {
            constructor.statements.add(new ExpressionStatementNode(new ListNode(null, TreeUtil.createIdentifier(name), -1)));
        }
        if (!liveCodingStarterAdded) {
            constructor.statements.add(new ExpressionStatementNode(new ListNode(null, TreeUtil.createIdentifier("LiveCodingSessionStarter", "prototype"), -1)));
            addLiveCodingStarterUnit(packageName, cx);
            liveCodingStarterAdded = true;
        }
        // We now do it in the live classes constructor
        /*
        for (String name : projectNavigator.getLiveCodingClassNames(packageName)) {
            constructor.statements.add(new ExpressionStatementNode(new ListNode(null, TreeUtil.createIdentifier(name, "prototype"), -1)));
        }
        */
        for (String name : ProvidedPackages.getClassNames()) {
            CallExpressionNode node = new CallExpressionNode(new IdentifierNode(name, -1), null);
            node.is_new = true;
            constructor.statements.add(new ExpressionStatementNode(new ListNode(null, new MemberExpressionNode(null, node, -1), -1)));
        }
        /* TODO: These imports does not help to resolve ModelDependencies classes, IDUNNO why. But we can skip it:

           If model A depends on model B, then there exists at least one class in A that refers to a class in B.
           Also that class in B refers to ModelDependencies_B. So ModelDependencies_B will surely get into project,
           even without direct reference in ModelDependencies_A

        for (String depName : projectNavigator.getModelDependencies(packageName)) {
            String depClassName = "ModelDependencies_" + depName.replaceAll("\\.", "_");
            CallExpressionNode node = new CallExpressionNode(new IdentifierNode(depClassName, -1), null);
            node.is_new = true;
            constructor.statements.add(new ExpressionStatementNode(new ListNode(null, new MemberExpressionNode(null, node, -1), -1)));
            classCONode.addImport(depName, depClassName);
        }
        */
        constructor.statements.add(new ReturnStatementNode(null));
        classCONode.methods.add(constructor);

        for (String name : ProvidedPackages.packages) {
            int lastDot = name.lastIndexOf(".");
            String importClassName = name.substring(lastDot + 1);
            String importPackageName = name.substring(0, lastDot);
            classCONode.addImport(importPackageName, importClassName);
        }

        classCONode.addToProject();
        return className;
    }

    private void addLiveCodingStarterUnit(String packageName, Context cx) {
        ClassCONode classCONode = new ClassCONode(packageName, "LiveCodingSessionStarter", cx);

        // COLT-41
        ArgumentListNode setSocketArgs = new ArgumentListNode(new LiteralStringNode(LiveCodingCLIParameters.getTraceHost()), -1);
        setSocketArgs.items.add(new LiteralNumberNode(String.valueOf(LiveCodingCLIParameters.getTracePort())));
        classCONode.staticInitializer.add(new ExpressionStatementNode(new ListNode(
                null,
                TreeUtil.createCall(
                        "LogUtil",
                        "setSocketAddress",
                        setSocketArgs
                ),
                -1
        )));

        classCONode.staticInitializer.add(new ExpressionStatementNode(new ListNode(
                null,
                TreeUtil.createCall(
                        "LiveCodingCodeFlowUtil",
                        "setMaxLoopCount",
                        new ArgumentListNode(new LiteralNumberNode(String.valueOf(LiveCodingCLIParameters.getMaxLoopIterations())), -1)
                ),
                -1
        )));

        classCONode.staticInitializer.add(new ExpressionStatementNode(new ListNode(
                null,
                TreeUtil.createCall(
                        "LiveCodingCodeFlowUtil",
                        "setMaxRecursionCount",
                        new ArgumentListNode(new LiteralNumberNode("100"), -1)
                ),
                -1
        )));

        classCONode.staticInitializer.add(new ExpressionStatementNode(new ListNode(
                null,
                new MemberExpressionNode(
                        TreeUtil.createCall("LiveCodeRegistry", "getInstance", null),
                        new CallExpressionNode(
                                new IdentifierNode("initSession", -1),
                                new ArgumentListNode(new LiteralStringNode(generateSessionId()), -1)
                        ),
                        -1
                ),
                -1
        )));
        classCONode.addImport("codeOrchestra.actionScript.logging.logUtil", "LogUtil");
        classCONode.addImport("codeOrchestra.actionScript.liveCoding.util", "LiveCodingCodeFlowUtil");
        classCONode.addImport("codeOrchestra.actionScript.liveCoding.util", "LiveCodeRegistry");
        classCONode.addToProject();
    }

    private static String generateSessionId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

}
