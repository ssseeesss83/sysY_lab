parser grammar SysYParser;

options{tokenVocab = SysYLexer;}

program : compUnit;

compUnit : ( decl | funcDef )+ EOF;

decl : constDecl | varDecl;

constDecl : CONST bType constDef (COMMA constDef SEMICOLON)*;

bType : INT;

constDef : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal;

constInitVal : constExp
| L_BRACE (constInitVal COMMA constInitVal)? R_BRACE;

varDecl : bType varDef (COMMA varDef)* SEMICOLON;

varDef : IDENT (L_BRACKT constExp R_BRACKT)*
| IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN initVal;

initVal : exp
| L_BRACE (initVal (COMMA initVal)*)? R_BRACE;

funcDef : funcType IDENT L_PAREN (funcFParams)? R_PAREN block;

funcType : VOID | INT;

funcFParams : param (COMMA param)*;

param: exp;

//funcFParam : bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)? ;

block : L_BRACE blockItem* R_BRACE;

blockItem : decl | stmt;

stmt : lVal ASSIGN exp SEMICOLON
| exp? SEMICOLON
| block
| IF L_PAREN cond R_PAREN stmt (ELSE stmt)?
| WHILE L_PAREN cond R_PAREN stmt
| BREAK SEMICOLON 
| CONTINUE SEMICOLON
| RETURN exp? SEMICOLON ;

exp : L_PAREN exp R_PAREN
| lVal
| number
| IDENT L_PAREN funcRParams? R_PAREN
| unaryOp exp
| exp (MUL | DIV | MOD) exp
| exp (PLUS | MINUS) exp;

cond : lOrExp;

lVal : IDENT (L_BRACKT exp R_BRACKT)*;

primaryExp : L_PAREN exp R_PAREN | lVal | number;

number : INTEGR_CONST;

//intConst: INTEGR_CONST;//一元表达式

//decimalConst : '0' | [1-9]DIGIT*;
//octalConst : '0'[1-7][0-7]*;
//hexadecimalConst :('0X'|'0x')(DIGIT|[a-fA-F])+);

unaryExp : primaryExp
| IDENT L_PAREN funcRParams? R_PAREN
| unaryOp unaryExp;

unaryOp : PLUS | MINUS | NOT ;//注：NOT仅出现在条件表达式中

funcRParams : exp  (COMMA exp)* ;

mulExp : unaryExp | mulExp (MUL | DIV | MOD) unaryExp;

addExp : mulExp | addExp (PLUS | MINUS) mulExp;

relExp : addExp | relExp (LT | GT | LE | GE) addExp;

eqExp : relExp | eqExp (EQ | NEQ) relExp;

lAndExp : eqExp | lAndExp AND eqExp;

lOrExp : lAndExp | lOrExp OR lAndExp;

constExp : addExp;// 注：使用的 IDENT 必须是常量
