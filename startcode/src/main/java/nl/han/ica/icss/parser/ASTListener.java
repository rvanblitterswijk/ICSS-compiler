package nl.han.ica.icss.parser;

import java.util.Stack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {
	
	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private Stack<ASTNode> currentContainer;

	public ASTListener() {
		ast = new AST();
		currentContainer = new Stack<>();
	}
    public AST getAST() {
        return ast;
    }

    @Override
    public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
        currentContainer.push(ast.root);
    }

    @Override
    public void enterIfClause(ICSSParser.IfClauseContext ctx) {
        IfClause ifClause = new IfClause();
        currentContainer.peek().addChild(ifClause);
        currentContainer.push(ifClause);
    }

    @Override
    public void exitIfClause(ICSSParser.IfClauseContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
        VariableAssignment variableAssignment = new VariableAssignment();
        currentContainer.peek().addChild(variableAssignment);
        currentContainer.push(variableAssignment);
    }

    @Override
    public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
	    currentContainer.pop();
    }

    @Override
    public void enterVariableReference(ICSSParser.VariableReferenceContext ctx) {
        currentContainer.peek().addChild(new VariableReference(ctx.getText()));
    }

    @Override
    public void enterBoolLiteral(ICSSParser.BoolLiteralContext ctx) {
        currentContainer.peek().addChild(new BoolLiteral(ctx.getText()));
    }

    @Override
    public void enterMinOrPlusOperation(ICSSParser.MinOrPlusOperationContext ctx) {
	    if (ctx.MIN() != null) {
            Operation minOperation = new SubtractOperation();
            currentContainer.peek().addChild(minOperation);
            currentContainer.push(minOperation);
        } else if (ctx.PLUS() != null) {
            Operation addOperation = new AddOperation();
            currentContainer.peek().addChild(addOperation);
            currentContainer.push(addOperation);
        }
    }

    @Override
    public void exitMinOrPlusOperation(ICSSParser.MinOrPlusOperationContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterMultiplyOperation(ICSSParser.MultiplyOperationContext ctx) {
        MultiplyOperation multiplyOperation = new MultiplyOperation();
        currentContainer.peek().addChild(multiplyOperation);
        currentContainer.push(multiplyOperation);
    }

    @Override
    public void exitMultiplyOperation(ICSSParser.MultiplyOperationContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterScalarLiteral(ICSSParser.ScalarLiteralContext ctx) {
        currentContainer.peek().addChild(new ScalarLiteral(ctx.getText()));
    }

    @Override
    public void enterStyleRule(ICSSParser.StyleRuleContext ctx) {
	    Stylerule stylerule = new Stylerule();
        ast.root.addChild(stylerule);
        currentContainer.push(stylerule);
    }

    @Override
    public void exitStyleRule(ICSSParser.StyleRuleContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterTagSelector(ICSSParser.TagSelectorContext ctx) {
        currentContainer.peek().addChild(new TagSelector(ctx.getText()));
    }

    @Override
    public void enterClassSelector(ICSSParser.ClassSelectorContext ctx) {
        currentContainer.peek().addChild(new ClassSelector(ctx.getText()));
    }

    @Override
    public void enterIdSelector(ICSSParser.IdSelectorContext ctx) {
        currentContainer.peek().addChild(new IdSelector(ctx.getText()));
    }

    @Override
    public void enterPropertyName(ICSSParser.PropertyNameContext ctx) {
        Declaration declaration = new Declaration(ctx.getText());
        currentContainer.peek().addChild(declaration);
        currentContainer.push(declaration);
    }

    @Override
    public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        currentContainer.pop();
    }

    @Override
    public void enterColorLiteral(ICSSParser.ColorLiteralContext ctx) {
        currentContainer.peek().addChild(new ColorLiteral(ctx.getText()));
    }

    @Override
    public void enterPixelLiteral(ICSSParser.PixelLiteralContext ctx) {
        currentContainer.peek().addChild(new PixelLiteral(ctx.getText()));
    }

    @Override
    public void enterPercentageLiteral(ICSSParser.PercentageLiteralContext ctx) {
        currentContainer.peek().addChild(new PercentageLiteral(ctx.getText()));
    }
}
