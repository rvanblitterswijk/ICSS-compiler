package nl.han.ica.icss.generator;

import jdk.jfr.Percentage;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

public class Generator {

    public String generate(AST ast) {
        StringBuilder output = new StringBuilder();
        for (ASTNode node : ast.root.body) {
            if (node instanceof Stylerule) {
                for (ASTNode sNode : node.getChildren()) {
                    if (sNode instanceof TagSelector) {
                        output.append(((TagSelector) sNode).tag);
                        output.append(" {").append("\n");
                    } else if (sNode instanceof ClassSelector) {
                        output.append(((ClassSelector) sNode).cls);
                        output.append(" {").append("\n");
                    } else if (sNode instanceof IdSelector) {
                        output.append(((IdSelector) sNode).id);
                        output.append(" {").append("\n");
                    } else if (sNode instanceof Declaration) {
                        output.append("    ");
                        output.append(((Declaration) sNode).property.name);
                        output.append(": ");
                        if (((Declaration) sNode).expression instanceof ColorLiteral) {
                            output.append(((ColorLiteral) ((Declaration) sNode).expression).value);
                        } else if (((Declaration) sNode).expression instanceof PercentageLiteral) {
                            output.append(((PercentageLiteral) ((Declaration) sNode).expression).value);
                            output.append("%");
                        } else if (((Declaration) sNode).expression instanceof PixelLiteral) {
                            output.append(((PixelLiteral) ((Declaration) sNode).expression).value);
                            output.append("px");
                        } else if (((Declaration) sNode).expression instanceof VariableReference) {
                            appendVariableValue(((VariableReference) ((Declaration) sNode).expression).name, ast, output);
                        }
                        output.append(";\n");
                    }
                }
                output.append("}").append("\n").append("\n");
            }
        }
        return output.toString();
    }

    private void appendVariableValue(String variableReference, AST ast, StringBuilder output) {
        int FIRST = 1;
        for (ASTNode node : ast.root.body) {
            if (node instanceof VariableAssignment) {
                for (ASTNode VANode : node.getChildren()) {
                    if (VANode instanceof VariableReference) {
                        if (((VariableReference) VANode).name.equals(variableReference)) {
                            if (node.getChildren().get(1) instanceof BoolLiteral) {
                                output.append(((BoolLiteral) node.getChildren().get(1)).value);
                            } else if (node.getChildren().get(1) instanceof PixelLiteral) {
                                output.append(((PixelLiteral) node.getChildren().get(1)).value);
                                output.append("px");
                            } else if (node.getChildren().get(1) instanceof ColorLiteral) {
                                output.append(((ColorLiteral) node.getChildren().get(1)).value);
                            } else if (node.getChildren().get(1) instanceof PercentageLiteral) {
                                output.append(((PercentageLiteral) node.getChildren().get(1)).value);
                                output.append("%");
                            } else if (node.getChildren().get(1) instanceof ScalarLiteral) {
                                output.append(((ScalarLiteral) node.getChildren().get(1)).value);
                            } else if (node.getChildren().get(1) instanceof VariableReference) {
                                appendVariableValue(variableReference, ast, output);
                            }
                        }
                    }
                }
            }
        }
    }
}
