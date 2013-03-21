package codeOrchestra;

import codeOrchestra.tree.*;
import flex2.compiler.CompilationUnit;
import macromedia.asc.parser.*;
import macromedia.asc.util.Context;
import macromedia.asc.util.ObjectList;

import java.util.*;

/**
 * @author Anton.I.Neverov
 */
public class TMExtension extends AbstractTreeModificationExtension {

    private Map<String, String> modelDependenciesUnits = new HashMap<String, String>();

    @Override
    protected void performModifications(CompilationUnit unit) {
        ClassDefinitionNode classDefinitionNode = TreeNavigator.getClassDefinition(unit);

        String packageName = classDefinitionNode.pkgdef.name.id.pkg_part;
        if (!modelDependenciesUnits.keySet().contains(packageName) && !ProvidedPackages.isProvidedPackage(packageName)) {
            String mdClassName = addModelDependenciesUnit(packageName, classDefinitionNode.cx);
            modelDependenciesUnits.put(packageName, mdClassName);
        }

        if (LiveCodingUtil.hasLiveAnnotation(classDefinitionNode)) {
            TreeUtil.addImport(unit, "codeOrchestra.actionScript.liveCoding.util", "LiveCodeRegistry");
            TreeUtil.addImport(unit, "codeOrchestra.actionScript.liveCoding.util", "MethodUpdateEvent");

            for (FunctionDefinitionNode methodDefinition : TreeNavigator.getMethodDefinitions(classDefinitionNode)) {
                if (LiveCodingUtil.hasLiveAnnotation(methodDefinition)) {
                    extractMethodToLiveCodingClass(methodDefinition, classDefinitionNode);
                }
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
        }
    }

    private void extractMethodToLiveCodingClass(FunctionDefinitionNode functionDefinitionNode, ClassDefinitionNode classDefinitionNode) {
        ObjectList<Node> oldBody = functionDefinitionNode.fexpr.body.items;
        functionDefinitionNode.fexpr.body.items = new ObjectList<Node>();

        fillStubMethodBody(functionDefinitionNode, classDefinitionNode.name.name);
        addLiveCodingClass(classDefinitionNode.name.name, functionDefinitionNode, oldBody);
        addListener(functionDefinitionNode, classDefinitionNode);
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
        String remoteMethodName = pkgdef.name.id.pkg_part + "." + className + "." + functionDefinitionNode.name.identifier.name;
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
        ListNode expr;
        if (!staticMethod) {
            CallExpressionNode callExpressionNode = new CallExpressionNode(
                    new IdentifierNode("method", -1),
                    new ArgumentListNode(new ThisExpressionNode(),-1)
            );
            callExpressionNode.is_new = true;
            ListNode base = new ListNode(null, new MemberExpressionNode(null, callExpressionNode, -1), -1);
            CallExpressionNode selector = new CallExpressionNode(new IdentifierNode("run", -1), null);
            expr = new ListNode(null, new MemberExpressionNode(base, selector, -1), -1);
        } else {
            expr = new ListNode(null, TreeUtil.createCall("method", "run", null), -1);
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

    private void addLiveCodingClass(String className, FunctionDefinitionNode functionDefinitionNode, ObjectList<Node> methodBody) {
        boolean staticMethod = TreeNavigator.isStaticMethod(functionDefinitionNode);
        TypeExpressionNode methodResult = (TypeExpressionNode) functionDefinitionNode.fexpr.signature.result;
        String liveCodingClassName = LiveCodingUtil.constructLiveCodingClassName(functionDefinitionNode, className);
        Context cx = functionDefinitionNode.cx;
        String packageName = functionDefinitionNode.pkgdef.name.id.pkg_part;
        ClassCONode classCONode = new ClassCONode(packageName, liveCodingClassName, cx);

        List<ImportDirectiveNode> imports = TreeNavigator.getImports(functionDefinitionNode.pkgdef);
        for (ImportDirectiveNode anImport : imports) {
            classCONode.addImport(anImport.name.id.pkg_part, anImport.name.id.def_part);
        }
        classCONode.addImport("codeOrchestra.actionScript.liveCoding.util", "LiveCodingCodeFlowUtil");
        classCONode.addImport("codeOrchestra.actionScript.liveCoding.util", "LiveCodeRegistry");

        /*
            {
              LiveCodeRegistry.getInstance().putMethod("com.example.Main.foo", LiveMethod_com_example_Main_foo);
            }
         */
        ArgumentListNode args = new ArgumentListNode(
                new LiteralStringNode(LiveCodingUtil.constructLiveCodingMethodId(functionDefinitionNode, className)),
                -1
        );
        args.items.add(TreeUtil.createIdentifier(liveCodingClassName));
        MemberExpressionNode memberExpressionNode = new MemberExpressionNode(
                TreeUtil.createCall("LiveCodeRegistry", "getInstance", null),
                new CallExpressionNode(new IdentifierNode("putMethod", -1), args),
                -1
        );
        ExpressionStatementNode expressionStatementNode = new ExpressionStatementNode(new ListNode(null, memberExpressionNode, -1));
        classCONode.staticInitializer.add(expressionStatementNode);

        /*
            public var thisScope : Main;
         */
        classCONode.fields.add(new FieldCONode("thisScope", className));

        /*
            public function LiveMethod_com_example_Main_foo( thisScope : Main ){
                this.thisScope = thisScope;
            }
         */
        if (!staticMethod) {
            MethodCONode constructor = new MethodCONode(liveCodingClassName, null, cx);
            constructor.addParameter("thisScope", className);
            memberExpressionNode = new MemberExpressionNode(
                    new ThisExpressionNode(),
                    new SetExpressionNode(
                            new IdentifierNode("thisScope", -1),
                            new ArgumentListNode(TreeUtil.createIdentifier("thisScope"), -1)
                    ),
                    -1
            );
            constructor.statements.add(new ListNode(null, memberExpressionNode, -1));
            constructor.statements.add(new ReturnStatementNode(null));
            classCONode.methods.add(constructor);
        }

        /*
            public function run (  ) : void {
                LiveCodingCodeFlowUtil.checkRecursion("com.example.LiveMethod_com_example_Main_foo.run");
                try {
                    ...
                } catch ( e : Error ) {

                }
                [return null;]
            }
         */

        MethodCONode runMethod = new MethodCONode(
                "run",
                methodResult == null ? null : ((IdentifierNode) ((MemberExpressionNode) methodResult.expr).selector.expr).name, // TODO: rewrite
                cx
        );
        runMethod.statements.add(new ExpressionStatementNode(new ListNode(null,
                TreeUtil.createCall(
                        "LiveCodingCodeFlowUtil",
                        "checkRecursion",
                        new ArgumentListNode(new LiteralStringNode(packageName + "." + liveCodingClassName + ".run"), -1)
                ),
                -1)));
        StatementListNode tryblock = new StatementListNode(null);
        tryblock.items.addAll(methodBody); // TODO: Method body is unmodified here!
        StatementListNode catchlist = new StatementListNode(new CatchClauseNode(
                TreeUtil.createParameterNode("e", "Error"),
                new StatementListNode(null))
        );
        runMethod.statements.add(new TryStatementNode(tryblock, catchlist, null));
        if (methodResult != null) {
            runMethod.statements.add(new ReturnStatementNode(new ListNode(null, new LiteralNullNode(), -1)));
        }
        runMethod.statements.add(new ReturnStatementNode(null));
        classCONode.methods.add(runMethod);

        classCONode.addToProject();
    }

    private void addListener(FunctionDefinitionNode functionDefinitionNode, ClassDefinitionNode classDefinitionNode) {
        /*
            public function foo_codeUpdateListener4703382380319456072 ( e : MethodUpdateEvent ) : void {
                if ( e.classFqn == "com.example.Main" ) {
                    foo();
                }
            }
         */
        boolean staticMethod = TreeNavigator.isStaticMethod(functionDefinitionNode);
        String id = UUID.randomUUID().toString(); // TODO: Just any unique id?
        String methodName = functionDefinitionNode.name.identifier.name;
        String listenerName = methodName + "_codeUpdateListener" + id;
        MethodCONode listener = new MethodCONode(listenerName, null, classDefinitionNode.cx);
        listener.addParameter("e", "MethodUpdateEvent");
        String className = classDefinitionNode.name.name;
        ListNode condition = new ListNode(
                null,
                new BinaryExpressionNode(
                        Tokens.EQUALS_TOKEN,
                        TreeUtil.createIdentifier("e", "classFqn"),
                        new LiteralStringNode(functionDefinitionNode.pkgdef.name.id.pkg_part + "." + className)
                ),
                -1
        );
        StatementListNode thenactions = new StatementListNode(new ExpressionStatementNode(new ListNode(
                null,
                TreeUtil.createCall(null, methodName, null),
                -1
        )));
        listener.statements.add(new IfStatementNode(condition, thenactions, null));
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
        MemberExpressionNode qListenerName = staticMethod ? TreeUtil.createIdentifier(className, listenerName) : TreeUtil.createThisIdentifier(listenerName);
        args.items.add(qListenerName);
        args.items.add(new LiteralBooleanNode(false));
        args.items.add(new LiteralNumberNode("0"));
        args.items.add(new LiteralBooleanNode(true));
        CallExpressionNode selector = new CallExpressionNode(new IdentifierNode("addEventListener", -1), args);
        MemberExpressionNode item = new MemberExpressionNode(TreeUtil.createCall("LiveCodeRegistry", "getInstance", null), selector, -1);
        constructorDefinition.fexpr.body.items.add(new ExpressionStatementNode(new ListNode(null, item, -1)));
    }

    private String addModelDependenciesUnit(String packageName, Context cx) {
        String className = "ModelDependencies_" + packageName.replaceAll("\\.", "_");
        ClassCONode classCONode = new ClassCONode(packageName, className, cx);

        MethodCONode constructor = new MethodCONode(className, null, cx);

        for (String name : projectNavigator.getClassNames(packageName)) {
            constructor.statements.add(new ExpressionStatementNode(new ListNode(null, TreeUtil.createIdentifier(name), -1)));
        }
        // TODO: LiveCodingStarter
        for (String name : projectNavigator.getLiveCodingClassNames(packageName)) {
            constructor.statements.add(new ExpressionStatementNode(new ListNode(null, TreeUtil.createIdentifier(name, "prototype"), -1)));
        }
        for (String name : ProvidedPackages.getClassNames()) {
            CallExpressionNode node = new CallExpressionNode(new IdentifierNode(name, -1), null);
            node.is_new = true;
            constructor.statements.add(new ExpressionStatementNode(new ListNode(null, new MemberExpressionNode(null, node, -1), -1)));
        }
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
}
