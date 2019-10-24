package nl.han.ica.icss.transforms;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.BoolLiteral;

public class RemoveIf implements Transform {

    @Override
    public void apply(AST ast) {
        transNode(ast.root, ast.root);
    }

    private void transNode(ASTNode currentNode, ASTNode parentNode) {
        if (currentNode instanceof IfClause) {
            if (((IfClause) currentNode).conditionalExpression instanceof BoolLiteral) {
                if (((BoolLiteral) ((IfClause) currentNode).conditionalExpression).value == true) {
                    addIfChildrenToParentNode((IfClause) currentNode, parentNode);
                    parentNode.removeChild(currentNode);
                } else {
                    parentNode.removeChild(currentNode);
                }
            }
        }

        callTransNodeOnChildren(currentNode);
    }

    private void addIfChildrenToParentNode(IfClause ifClauseNode, ASTNode parentNode) {
        if (ifClauseNode.getChildren().size() > 0) {
            for (ASTNode ifChildNode : ifClauseNode.getChildren()) {
                if (!(ifChildNode instanceof VariableReference || ifChildNode instanceof BoolLiteral || ifChildNode instanceof IfClause)) {
                    parentNode.addChild(ifChildNode);
                } else if (ifChildNode instanceof IfClause) {
                    addIfChildrenToParentNode((IfClause) ifChildNode, ifClauseNode);
                    ifClauseNode.body.remove(ifChildNode);
                    addIfChildrenToParentNode(ifClauseNode, parentNode);
                }
            }
        }
    }

    private void callTransNodeOnChildren(ASTNode currentNode) {
        if (currentNode.getChildren().size() > 0) {
            for (ASTNode node : currentNode.getChildren()) {
                transNode(node, currentNode);
            }
        }
    }
}
