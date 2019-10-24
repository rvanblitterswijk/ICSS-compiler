grammar ICSS;

//--- LEXER: ---
// IF support:
IF: 'if';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';

//Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;

//Color value takes precedence over id idents
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

//Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

//General identifiers
LOWER_IDENT: [a-z0-9\-]+;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

//All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

//
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';

//--- PARSER: ---
stylesheet: variableAssignment* styleRule*;
styleRule: selector OPEN_BRACE (variableAssignment | declaration | ifClause)+ CLOSE_BRACE;
selector: tagSelector | classSelector | idSelector;

ifClause: IF BOX_BRACKET_OPEN ifExpression BOX_BRACKET_CLOSE OPEN_BRACE (variableAssignment | declaration | ifClause)+ CLOSE_BRACE ;
ifExpression: boolLiteral | variableReference;

variableAssignment: variableReference ASSIGNMENT_OPERATOR variableValue SEMICOLON;
variableReference: CAPITAL_IDENT;
variableValue: propertyValue | boolLiteral;

tagSelector: LOWER_IDENT;
classSelector: CLASS_IDENT;
idSelector: ID_IDENT;

declaration: propertyName COLON propertyExpression SEMICOLON;
propertyExpression: propertyValue | operation | variableReference;
operation: operation MUL operation #multiplyOperation
    | operation (MIN|PLUS) operation #minOrPlusOperation
    | propertyValue #literal;

propertyValue: colorLiteral | pixelLiteral | variableReference | percentageLiteral | scalarLiteral;
propertyName: LOWER_IDENT;

colorLiteral: COLOR;
boolLiteral: TRUE | FALSE;
pixelLiteral: PIXELSIZE;
scalarLiteral: SCALAR;
percentageLiteral: PERCENTAGE;


