package nl.han.ica.icss.transforms;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;

import java.util.HashMap;
import java.util.LinkedList;

public class EvalExpressions implements Transform {

    private LinkedList<HashMap<String, Literal>> variableValues;

    public EvalExpressions() {
        variableValues = new LinkedList<>();
    }

    @Override
    public void apply(AST ast) {
        variableValues = new LinkedList<>();
        ASTNode parentNode = ast.root;
        transformNodes(ast.root, parentNode);
    }

    private void transformNodes(ASTNode node, ASTNode parentNode) {
        if (node instanceof Stylesheet) {
            collectVariableValuesInStyleSheetScope(node);
        } else if (node instanceof Stylerule) {
            removeVariablesFromStyleruleScope();
            collectVariableValuesInStyleruleScope(node);
        } else if (node instanceof VariableReference) {
        } else if (node instanceof Operation) {
        } else if (node instanceof Literal) {
        }
        if (node.getChildren().size() > 0) {
            for (ASTNode nextNode : node.getChildren()) {
                transformNodes(nextNode, parentNode);
            }
        }
    }

    private void collectVariableValuesInStyleSheetScope(ASTNode stylesheetNode) {
        HashMap<String, Literal> globalVariablesMap = new HashMap<>();
        variableValues.addFirst(collectVariables(stylesheetNode, globalVariablesMap));
    }

    private void collectVariableValuesInStyleruleScope(ASTNode styleruleNode) {
        HashMap<String, Literal> styleruleVariablesMap = new HashMap<>();
        variableValues.addLast(collectVariables(styleruleNode, styleruleVariablesMap));
    }

    private HashMap<String, Literal> collectVariables(ASTNode parentNode, HashMap<String, Literal> hashMap) {
        for (ASTNode childNode : parentNode.getChildren()) {
            if (childNode instanceof VariableAssignment) {
                if (((VariableAssignment) childNode).expression instanceof Literal) {
                    hashMap.put(((VariableAssignment) childNode).name.name, ((Literal) ((VariableAssignment) childNode).expression));
                }
            }
        }
        for (ASTNode childNode : parentNode.getChildren()) {
            if (childNode instanceof VariableAssignment) {
                if (((VariableAssignment) childNode).expression instanceof VariableReference) {
                    for (ASTNode otherChildNode : childNode.getChildren()) {
                        if (otherChildNode instanceof VariableAssignment) {
                            if (((VariableAssignment) otherChildNode).name.name.equals(((VariableAssignment) childNode).expression)) {
                                hashMap.put(((VariableAssignment) childNode).name.name, ((Literal)((VariableAssignment) otherChildNode).expression));
                            }
                        }
                    }
                }
            }
        }
        return hashMap;
    }

    private void removeVariablesFromStyleruleScope() {
        while (variableValues.size() > 1) {
            variableValues.removeLast();
        }
    }
}
