package codeOrchestra.tree.visitor;

import macromedia.asc.parser.SuperExpressionNode;

/**
 * @author Anton.I.Neverov
 */
public class SuperExpressionNodeVisitor extends NodeVisitor<SuperExpressionNode> {
    @Override
    protected StuffToCompare createStuffToCompare(SuperExpressionNode left, SuperExpressionNode right) {
        StuffToCompare stuffToCompare = new StuffToCompare();

        stuffToCompare.leftChildren.add(left.expr);
        stuffToCompare.rightChildren.add(right.expr);

        return stuffToCompare;
    }
}
