package nl.han.ica.icss.checker;

import java.util.HashMap;
import java.util.LinkedList;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.*;

public class Checker {

    public LinkedList<HashMap<String, ExpressionType>> variableTypes = new LinkedList<>();

    public void check(AST ast) {
        ASTNode parentNode = ast.root;
        checkNodes(ast.root, parentNode, ast);
    }

    private void checkNodes(ASTNode node, ASTNode parentNode, AST ast) {
        if (node instanceof Stylesheet) {
            collectVariableTypesInGlobalScope(node);
        } else if (node instanceof Stylerule) {
            removeVariablesFromStyleruleScope();
            collectVariableTypesInStyleruleScope(node);
        } else if (node instanceof IfClause) {
            checkIfConditionInIfClauseIsBoolean(node, ast);
        } else if (node instanceof VariableReference) {
            checkIfVariablesAreDeclared(node);
        } else if (node instanceof Declaration) {
            checkIfDeclarationTypesAreCorrect((Declaration) node, ast);
        }

        if (node.getChildren().size() > 0) {
            for (ASTNode nextNode : node.getChildren()) {
                checkNodes(nextNode, node, ast);
            }
        }

    }

    private void checkIfDeclarationTypesAreCorrect(Declaration node, AST ast) {
            if ((node).property.name.equals("width") || (node).property.name.equals("height")) {
                if ((node).expression instanceof VariableReference) {
                    if (!(variableIsPixelOrPercentageLiteral(((VariableReference) (node).expression).name, ast))) {
                        node.setError("Value type in declaration is incorrect");
                    }
                } else if ((node).expression instanceof AddOperation ||
                        (node).expression instanceof MultiplyOperation ||
                        (node).expression instanceof SubtractOperation) {
                    for (ASTNode nextNode : node.getChildren()) {
                        if (nextNode instanceof Declaration) {
                            checkIfDeclarationTypesAreCorrect((Declaration) nextNode, ast);
                        }
                    }
                } else  if (!((node).expression instanceof PixelLiteral || (node).expression instanceof PercentageLiteral)) {
                    node.setError("Value type in declaration is incorrect");
                }
            } else if ((node).property.name.equals("background-color") || (node).property.name.equals("color")) {
                if ((node).expression instanceof VariableReference) {
                    if (!(variableIsColorLiteral(((VariableReference) (node).expression).name, ast))) {
                        node.setError("Value type in declaration is incorrect");
                    }
                }else if ((node).expression instanceof AddOperation ||
                        (node).expression instanceof MultiplyOperation ||
                        (node).expression instanceof SubtractOperation) {
                    for (ASTNode nextNode : node.getChildren()) {
                        if (nextNode instanceof Declaration) {
                            checkIfDeclarationTypesAreCorrect((Declaration) nextNode, ast);
                        }
                    }
                } else if (!((node).expression instanceof ColorLiteral)) {
                    node.setError("Value type in declaration is incorrect");
                }
            }
        }

    private boolean variableIsColorLiteral(String variableName, AST ast) {
        for (HashMap<String, ExpressionType> hashmap : variableTypes) {
            if (hashmap.get(variableName) == ExpressionType.COLOR) {
                return true;
            } else if (hashmap.get(variableName) == ExpressionType.VARIABLEREFERENCE) {
                return variableIsColorLiteral(getReferencedVariableInExpressionName(variableName, ast.root), ast);
            }
        }
        return false;
    }

    private boolean variableIsPixelOrPercentageLiteral(String variableName, AST ast) {
        for (HashMap<String, ExpressionType> hashmap : variableTypes) {
            if ((hashmap.get(variableName) == ExpressionType.PIXEL) || (hashmap.get(variableName) == ExpressionType.PERCENTAGE)) {
                return true;
            } else if (hashmap.get(variableName) == ExpressionType.VARIABLEREFERENCE) {
                return variableIsPixelOrPercentageLiteral(getReferencedVariableInExpressionName(variableName, ast.root), ast);
            }
        }
        return false;
    }

    /*
    Controleer of er geen variabelen worden gebruikt die niet gedefinieerd zijn.
     */
    private void checkIfVariablesAreDeclared(ASTNode node) {
        if (!(variableIsDefined(((VariableReference) node).name))) {
            node.setError("The referenced variable " + ((VariableReference) node).name + " has not been defined");
        }
    }

    private boolean variableIsDefined(String variableName) {
        for (HashMap<String, ExpressionType> variable : variableTypes) {
            if (variable.containsKey(variableName)) {
                return true;
            }
        }
        return false;
    }

    /*
    Controleer of de conditie bij een if-statement van het type boolean is (zowel bij een variabele-referentie als een boolean literal)
     */
    private void checkIfConditionInIfClauseIsBoolean(ASTNode ifNode, AST ast) {
        ASTNode conditionNode = ifNode.getChildren().get(0);
        if (conditionNode instanceof VariableReference) {
            if (!(variableIsOfBoolType(((VariableReference) conditionNode).name, ast))) {
                ifNode.setError("Conditions within an if statement need to be a boolean literal or variable reference to a boolean variable.");
            }
        } else if (!(conditionNode instanceof BoolLiteral)) {
            ifNode.setError("Conditions within an if statement need to be a boolean literal or variable reference to a boolean variable");
        }
    }

    private boolean variableIsOfBoolType(String variableName, AST ast) {
        for (HashMap<String, ExpressionType> hashmap : variableTypes) {
            if (hashmap.get(variableName) == ExpressionType.BOOL) {
                return true;
            } else if (hashmap.get(variableName) == ExpressionType.VARIABLEREFERENCE) {
                if (variableIsOfBoolType(getReferencedVariableInExpressionName(variableName, ast.root), ast)) return true;
            }
        }
        return false;
    }

    private String getReferencedVariableInExpressionName(String variableName, ASTNode node) {
        for (ASTNode childNode : node.getChildren()) {
            if (childNode instanceof VariableAssignment) {
                if (((VariableAssignment) childNode).name.name.equals(variableName)) {
                    if (((VariableAssignment) childNode).expression instanceof VariableReference) {
                        return ((VariableReference) ((VariableAssignment) childNode).expression).name;
                    }
                }
            }
        }
        return "";
    }

    private void removeVariablesFromStyleruleScope() {
        while (variableTypes.size() > 1) {
            variableTypes.removeLast();
        }
    }

    private void collectVariableTypesInGlobalScope(ASTNode stylesheetNode) {
        HashMap<String, ExpressionType> globalVariablesMap = new HashMap<>();
        variableTypes.addFirst(addVariableReferenceAndTypeToHashmap(stylesheetNode, globalVariablesMap));
    }

    private void collectVariableTypesInStyleruleScope(ASTNode styleruleNode) {
        HashMap<String, ExpressionType> styleruleVariablesMap = new HashMap<>();
        variableTypes.addLast(addVariableReferenceAndTypeToHashmap(styleruleNode, styleruleVariablesMap));
    }

    private HashMap<String, ExpressionType> addVariableReferenceAndTypeToHashmap(ASTNode parentNode, HashMap<String, ExpressionType> hashMap) {
        for (ASTNode parentNodeChild : parentNode.getChildren()) {
            if (parentNodeChild instanceof Stylerule) return hashMap;
            if (parentNodeChild instanceof VariableAssignment) {
                if (((VariableAssignment) parentNodeChild).expression instanceof PixelLiteral) {
                    hashMap.put(((VariableAssignment) parentNodeChild).name.name, ExpressionType.PIXEL);
                } else if (((VariableAssignment) parentNodeChild).expression instanceof PercentageLiteral) {
                    hashMap.put(((VariableAssignment) parentNodeChild).name.name, ExpressionType.PERCENTAGE);
                } else if (((VariableAssignment) parentNodeChild).expression instanceof ColorLiteral) {
                    hashMap.put(((VariableAssignment) parentNodeChild).name.name, ExpressionType.COLOR);
                } else if (((VariableAssignment) parentNodeChild).expression instanceof ScalarLiteral) {
                    hashMap.put(((VariableAssignment) parentNodeChild).name.name, ExpressionType.SCALAR);
                } else if (((VariableAssignment) parentNodeChild).expression instanceof BoolLiteral) {
                    hashMap.put(((VariableAssignment) parentNodeChild).name.name, ExpressionType.BOOL);
                } else if (((VariableAssignment) parentNodeChild).expression instanceof Operation) {
                    hashMap.put(((VariableAssignment) parentNodeChild).name.name, ExpressionType.OPERATION);
                } else if (((VariableAssignment) parentNodeChild).expression instanceof VariableReference) {
                    hashMap.put(((VariableAssignment) parentNodeChild).name.name, ExpressionType.VARIABLEREFERENCE);
                } else {
                    hashMap.put(((VariableAssignment) parentNodeChild).name.name, ExpressionType.UNDEFINED);
                }
            }
        }
        return hashMap;
    }
}
