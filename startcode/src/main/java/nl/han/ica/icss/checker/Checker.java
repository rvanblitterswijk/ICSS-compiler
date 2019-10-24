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
        System.out.println(variableTypes);
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





    /*
    Controleer of de operanden van de operaties plus en min van gelijk type zijn en dat vermenigvuldigen enkel met scalaire waarden gebeurt. Je mag geen pixels bij percentages optellen bijvoorbeeld.
     */
    /*
    private void checkOperandTypesInOperations(ASTNode node) {
        if (node.getChildren().size() > 0) {
            if (node instanceof AddOperation || node instanceof SubtractOperation) {
                if (!(operandsAreOfSameType(node.getChildren().get(0), node.getChildren().get(1)))) {
                    node.setError("Operands in the add/subtract operation are not of the same type");
                }
            } else if (node instanceof MultiplyOperation) {
                if (!(operandsAreOfScalarType(node.getChildren().get(0), node.getChildren().get(1)))) {
                    node.setError("Operands in the multiply operation can only be of scalar literal type");
                }
            }
            for (ASTNode nextNode : node.getChildren()) {
                checkOperandTypesInOperations(nextNode);
            }
        }

    }
    private boolean operandsAreOfScalarType(ASTNode operand1, ASTNode operand2) {
    }

    private boolean operandsAreOfSameType(ASTNode operand1, ASTNode operand2) {
        if (operand1 instanceof MultiplyOperation) {
            if (operandsAreOfScalarType(operand1.getChildren().get(0), operand1.getChildren().get(1))) {
                if (operand2 instanceof ScalarLiteral) {
                    return true;
                } else if (operand2 instanceof MultiplyOperation) {
                    if (operandsAreOfScalarType(operand2.getChildren().get(0), operand2.getChildren().get(1))) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (operand1 instanceof AddOperation || operand1 instanceof SubtractOperation) {
            if (operandsAreOfSameType(operand1.getChildren().get(0), operand1.getChildren().get(1))) {
                if (operand2.getClass() == operand1.getChildren().get(0).getClass() ) {
                    return true;
                } else if (operand2 instanceof AddOperation || operand2 instanceof SubtractOperation) {
                    if (operandsAreOfSameType(operand2.getChildren().get(0), operand2.getChildren().get(1))) {
                        if (operand2.getChildren().get(0).getClass() == operand1.getChildren().get(0).getClass()) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (operand1 instanceof VariableReference) {
            if ( == getVariableType(operand1)) {
                return true;
            } else {
                return false;
            }
        } else if (operand1 instanceof ScalarLiteral) {

        } else if (operand1 instanceof PercentageLiteral) {

        } else if (operand1 instanceof ColorLiteral) {

        } else if (operand1 instanceof PixelLiteral) {

        } else if (operand1 instanceof )
        return false;
    }*/

    /*private ExpressionType getVariableType(ASTNode operand1) {
        for (HashMap<String, ExpressionType> variable : variableTypes) {
            if (variable.containsKey(operand1)) {
                return variable.get(operand1);
            }
        }
        return null;
    }*/



    /*
    Controleer of bij declaraties het type van de waarde klopt bij de stijleigenschap. Declaraties zoals width: #ff0000 of color: 12px zijn natuurlijk onzin.
     */
    /*private void checkIfDeclarationTypesAreCorrect(ASTNode node) {
        if (node.getChildren().size() > 0) { // If this node has children
            if (node instanceof Declaration) { // If the node is an instanceof a declaration
                if (((Declaration) node).property.name.equals("width") || ((Declaration) node).property.name.equals("height")) {
                    if (((Declaration) node).expression instanceof VariableReference) {
                        if (!(isPixelOrPercentageLiteral(((VariableReference) ((Declaration) node).expression).name))) {
                            node.setError("Value type in declaration is incorrect");
                        }
                    } else if (((Declaration) node).expression instanceof AddOperation ||
                            ((Declaration) node).expression instanceof MultiplyOperation ||
                            ((Declaration) node).expression instanceof SubtractOperation) {
                        for (ASTNode nextNode : node.getChildren()) {
                            checkIfDeclarationTypesAreCorrect(nextNode);
                        }
                    } else  if (!(((Declaration) node).expression instanceof PixelLiteral || ((Declaration) node).expression instanceof PercentageLiteral)) {
                        node.setError("Value type in declaration is incorrect");
                    }
                } else if (((Declaration) node).property.name.equals("background-color") || ((Declaration) node).property.name.equals("color")) {
                    if (((Declaration) node).expression instanceof VariableReference) {
                        if (!(isColorLiteral(((VariableReference) ((Declaration) node).expression).name))) {
                            node.setError("Value type in declaration is incorrect");
                        }
                    } else if (((Declaration) node).expression instanceof AddOperation ||
                            ((Declaration) node).expression instanceof MultiplyOperation ||
                            ((Declaration) node).expression instanceof SubtractOperation) {
                        for (ASTNode nextNode : node.getChildren()) {
                            checkIfDeclarationTypesAreCorrect(nextNode);
                        }
                    } else if (!(((Declaration) node).expression instanceof ColorLiteral)) {
                        node.setError("Value type in declaration is incorrect");
                    }
                }
            }
            for (ASTNode nextNode : node.getChildren()) {
                checkIfDeclarationTypesAreCorrect(nextNode);
            }
        }
    }
}*/
