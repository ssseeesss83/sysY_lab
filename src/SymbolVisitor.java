
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class SymbolVisitor extends SysYParserBaseVisitor<Void>{
    public boolean hasError = false;
    private BaseScope currentScope;
    private int localScopeCnt;
    boolean isFunctionBlock = false; //用于遍历标志是否是函数的括号作用域
    boolean visitFunctionBlock = true;
    BaseType currentFuncRetType = null;


    public int lineNo;
    public int column;
    public String name;
    public Symbol toRename;

    private Map<String, Symbol> symbolTable = new HashMap<>();

    private boolean isDeclaredAtSameScope(String name){
        return symbolTable.containsKey(formatName(name));
    }

    //this is nullable.
    private Symbol getSymbol(String name){
        BaseScope scope = currentScope;
        while (scope!=null){
            if(symbolTable.containsKey(formatName(name, scope))){
                return symbolTable.get(formatName(name, scope));
            }
            scope = scope.getParent();
        }
        return null;
    }

    private boolean isDeclared(String name){
        return getSymbol(name)!=null;
    }


    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        super.visitStmt(ctx);
        if(ctx.lVal()!=null){
            if(getSymbol(ctx.lVal().IDENT().getText())==null){
                return null;
            }
            BaseType lType = Objects.requireNonNull(getSymbol(ctx.lVal().IDENT().getText())).getType();
            if(!(lType instanceof FunctionType)) {
                BaseType rType = null;
                //右值的状况
                SysYParser.ExpContext exp = ctx.exp();
                if (exp instanceof SysYParser.ExpParenthesisContext) {
                    while (exp instanceof SysYParser.ExpParenthesisContext) {
                        exp = ((SysYParser.ExpParenthesisContext) exp).exp();
                    }
                }
                if (exp instanceof SysYParser.NumberExpContext) {
                    if (!(lType instanceof PrimaryType)) {
                        System.err.println("Error type 5 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ":Type mismatched:" + ctx.lVal().IDENT().getText());
                        hasError = true;
                        return null;
                    }
                } else if (exp instanceof SysYParser.LvalExpContext) {
                    if (getSymbol(((SysYParser.LvalExpContext) exp).lVal().IDENT().getText()) == null) { //未定义直接返回，错误已报。
                        return null;
                    }
                    rType = Objects.requireNonNull(getSymbol(((SysYParser.LvalExpContext) exp).lVal().IDENT().getText())).getType();
                    if (lType instanceof ArrayType) {
                        int lSize = ((ArrayType) lType).getDim() - ctx.lVal().exp().size() + 1; //需要的右值数组维数
                        if (lSize == 1 && !(rType instanceof PrimaryType)
                                || (lSize > 1 && rType instanceof ArrayType && lSize != ((ArrayType) rType).getDim())) {
                            System.err.println("Error type 5 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ":Type mismatched:" + ctx.lVal().IDENT().getText());
                            hasError = true;
                            return null;
                        }
                    } else {
                        if (!lType.equals(rType)) {
                            System.err.println("Error type 5 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ":Type mismatched:" + ctx.lVal().IDENT().getText());
                            hasError = true;
                            return null;
                        }
                    }
                } else if (exp instanceof SysYParser.CallFuncExpContext) {
                    if (getSymbol(((SysYParser.CallFuncExpContext) exp).IDENT().getText()) == null) {
                        return null;
                    }
                    rType = Objects.requireNonNull(getSymbol(((SysYParser.CallFuncExpContext) exp).IDENT().getText())).getType();
                    if (!lType.equals(rType)) {
                        System.err.println("Error type 5 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ":Type mismatched:" + ctx.lVal().IDENT().getText());
                        hasError = true;
                        return null;
                    }
                }
//            else if(ctx.exp() instanceof SysYParser.MulExpContext
//            || ctx.exp() instanceof SysYParser.PlusExpContext){
//                if(!(lType instanceof PrimaryType)){
//                    System.err.println("Error type 5 at Line "+ctx.lVal().IDENT().getSymbol().getLine()+":Type mismatched:"+ctx.lVal().IDENT().getText());
//                    return null;
//                }
//            }
                else {
                    if (!(lType instanceof PrimaryType)) {
                        System.err.println("Error type 5 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ":Type mismatched:" + ctx.lVal().IDENT().getText());
                        hasError = true;
                        return null;
                    }
                }

            }else{
                System.err.println("Error type 11 at Line " + ctx.lVal().IDENT().getSymbol().getLine() + ":LVal cannot be a function:" + ctx.lVal().IDENT().getText());
                hasError = true;
            }
        }
        return null;
    }

    @Override
    public Void visitCallFuncExp(SysYParser.CallFuncExpContext ctx) {
        if(!isDeclared(ctx.IDENT().getText())){
            System.err.println("Error type 2 at Line "+ctx.IDENT().getSymbol().getLine()+":Undefined function name:"+ctx.IDENT().getText());
            hasError = true;
            return super.visitCallFuncExp(ctx);
        }
        Symbol func = getSymbol(ctx.IDENT().getText());
        if(func != null && func.getType() instanceof FunctionType) {
            FunctionType funcType = (FunctionType) func.getType();
            List<SysYParser.ParamContext> rParams = new ArrayList<>();
            if(ctx.funcRParams()!=null) {
                rParams = ctx.funcRParams().param();
            }
            if(rParams.size() != funcType.getParams().size()){
                System.err.println("Error type 8 at Line "+ctx.IDENT().getSymbol().getLine()+":Function arguments type mismatched:"+ctx.IDENT().getText());
                hasError = true;
                return super.visitCallFuncExp(ctx);
            }else{
                for(int i = 0 ; i < rParams.size(); i++){
                    if(!getExpType(rParams.get(i).exp()).equals(funcType.getParams().get(i))){
                        System.err.println("Error type 8 at Line "+ctx.IDENT().getSymbol().getLine()+":Function arguments type mismatched:"+ctx.IDENT().getText());
                        hasError = true;
                        return super.visitCallFuncExp(ctx);
                    }
                }
            }
        }else{
            System.err.println("Error type 10 at Line "+ctx.IDENT().getSymbol().getLine()+":Not a function:"+ctx.IDENT().getText());
            hasError = true;
        }

        return super.visitCallFuncExp(ctx);
    }

    public BaseType getExpType(SysYParser.ExpContext exp){
        if(exp instanceof SysYParser.PlusExpContext){
            BaseType typeA = getExpType(((SysYParser.PlusExpContext) exp).exp(0));
            BaseType typeB = getExpType(((SysYParser.PlusExpContext) exp).exp(1));
            if(typeA == null || typeB == null){
                return null;
            }
            if(typeA.equals(typeB)){
                return typeA;
//            if(typeA instanceof PrimaryType && typeB instanceof PrimaryType){
//                return typeA;
//            }else if(typeA instanceof PrimaryType && typeB instanceof FunctionType && typeB.e){
//
            }else{
                return null;
            }
        }else if(exp instanceof SysYParser.MulExpContext){
            BaseType typeA = getExpType(((SysYParser.MulExpContext) exp).exp(0));
            BaseType typeB = getExpType(((SysYParser.MulExpContext) exp).exp(1));
            if(typeA.equals(typeB)){
                return typeA;
            }else{
                return null;
            }
        }else if(exp instanceof SysYParser.LvalExpContext){
            if(getSymbol(((SysYParser.LvalExpContext) exp).lVal().IDENT().getText())!=null){
                return getSymbol(((SysYParser.LvalExpContext) exp).lVal().IDENT().getText()).getType();
            }
            return null;
        }else if(exp instanceof SysYParser.UnaryOpExpContext){
            return getExpType(((SysYParser.UnaryOpExpContext) exp).exp());
        }else if(exp instanceof SysYParser.NumberExpContext){
            return new PrimaryType();
        }else if(exp instanceof SysYParser.CallFuncExpContext){
            if(getSymbol(((SysYParser.CallFuncExpContext) exp).IDENT().getText())!=null){
                return ((FunctionType) getSymbol(((SysYParser.CallFuncExpContext) exp).IDENT().getText()).getType()).getReturnType();
            }
            return null;
        }else if(exp instanceof SysYParser.ExpParenthesisContext){
            return getExpType(((SysYParser.ExpParenthesisContext) exp).exp());
        }
        return null;
    }

    @Override
    public Void visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        super.visitUnaryOpExp(ctx);
        if(getExpType(ctx)==null){
            System.err.println("Error type 6 at Line "+ ((TerminalNode) ctx.unaryOp().getChild(0)).getSymbol().getLine()+":Type mismatched for op.");
            hasError = true;
        }
        return null;
    }

    @Override
    public Void visitPlusExp(SysYParser.PlusExpContext ctx) {
        super.visitPlusExp(ctx);
        if(getExpType(ctx)==null && !(ctx.getParent() instanceof SysYParser.PlusExpContext)){
            System.err.println("Error type 6 at Line "+ ((TerminalNode) ctx.getChild(1)).getSymbol().getLine()+":Type mismatched for op.");
            hasError = true;
        }
        return null;
    }

    @Override
    public Void visitMulExp(SysYParser.MulExpContext ctx) {
        super.visitMulExp(ctx);
        if(getExpType(ctx)==null && !(ctx.getParent() instanceof SysYParser.MulExpContext)){
            System.err.println("Error type 6 at Line "+ ((TerminalNode) ctx.getChild(1)).getSymbol().getLine()+":Type mismatched for op.");
            hasError = true;
        }
        return null;
    }

    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        if(!isDeclared(ctx.IDENT().getText())){
            System.err.println("Error type 1 at Line "+ctx.IDENT().getSymbol().getLine()+":Undefined variable:"+ctx.IDENT().getText());
            hasError = true;
        }
        if(ctx.L_BRACKT().size()>0){
            if(getSymbol(ctx.IDENT().getText())!=null){
                if(getSymbol(ctx.IDENT().getText()).getType() instanceof ArrayType){
                    if(((ArrayType) getSymbol(ctx.IDENT().getText()).getType()).getDim() < ctx.L_BRACKT().size()){
                        System.err.println("Error type 9 at Line "+ctx.IDENT().getSymbol().getLine()+":Not an array/array dims is lower than given:"+ctx.IDENT().getText());
                        hasError = true;
                    }
                }else{
                    System.err.println("Error type 9 at Line "+ctx.IDENT().getSymbol().getLine()+":Not an array/array dims is lower than given:"+ctx.IDENT().getText());
                    hasError = true;
                }
            }
        }
        return super.visitLVal(ctx);
    }

    private String formatName(String name){
        return currentScope.toString()+"_"+name;
    }

    private String formatName(String name, BaseScope scope){
        return scope.toString()+"_"+name;
    }

    public Map<String, Symbol> getSymbolTable() {
        return symbolTable;
    }

    @Override
    public Void visitCompUnit(SysYParser.CompUnitContext ctx) {
        currentScope = new GlobalScope();
        return super.visitCompUnit(ctx);
    }

    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        List<SysYParser.ConstDefContext> constDefs = ctx.constDef();
        for (SysYParser.ConstDefContext constDef:constDefs
             ) {
            declHandler(constDef.IDENT(), constDef.constExp());
        }
        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        List<SysYParser.VarDefContext> varDefs = ctx.varDef();
        for (SysYParser.VarDefContext varDef:varDefs
        ) {
            declHandler(varDef.IDENT(), varDef.constExp());
        }
        return super.visitVarDecl(ctx);
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        isFunctionBlock = true;
        visitFunctionBlock = true;
        String name = ctx.IDENT().getText();
        if(isDeclaredAtSameScope(name)){
            System.err.println("Error type 4 at Line "+ctx.IDENT().getSymbol().getLine()+":Redefined Function or Global variable:"+name);
            hasError = true;
            visitFunctionBlock = false;
            return super.visitFuncDef(ctx);
        }
        FunctionType type = new FunctionType(ctx.funcType().getText().equals("void")?null:new PrimaryType());
        currentFuncRetType = type.getReturnType();
        FunctionScope scope = new FunctionScope(name);
        scope.setParent(currentScope);
        currentScope = scope;
        if(ctx.funcFParams() != null) {
            for (SysYParser.FuncFParamContext param : ctx.funcFParams().funcFParam() //遍历形参
            ) {
                type.addParamType(new PrimaryType());
                String paramName = param.IDENT().getText();
                Symbol paramSymbol = new Symbol(paramName, new PrimaryType(), currentScope);
                symbolTable.put(formatName(paramName), paramSymbol);
            }
        }

        super.visitFuncDef(ctx);

        //return type check:
//        BaseType retType = getExpType(ctx.block().blockItem(ctx.block().blockItem().size()-1).stmt().exp());//最后一条blockitem
//        if(!retType.equals(type.getReturnType())){
//            hasError = true;
//            System.err.println("Error type 7 at Line "+ (ctx.block().blockItem(ctx.block().blockItem().size()-1).stmt().RETURN()).getSymbol().getLine()+":Type mismatched for return type.");
//        }
        isFunctionBlock = false;
        currentScope = currentScope.getParent();
        Symbol symbol = new Symbol(name, type, currentScope);
        symbolTable.put(formatName(name), symbol);
        return null;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        if(isFunctionBlock){
            isFunctionBlock = false;
            super.visitBlock(ctx);
            if(visitFunctionBlock) {
                if(ctx.blockItem().size()>0 && ctx.blockItem(ctx.blockItem().size() - 1).stmt()!=null && ctx.blockItem(ctx.blockItem().size() - 1).stmt().RETURN()!=null) {
                    BaseType retType = getExpType(ctx.blockItem(ctx.blockItem().size() - 1).stmt().exp());//最后一条blockitem
                    if (!retType.equals(currentFuncRetType)) {
                        hasError = true;
                        System.err.println("Error type 7 at Line " + (ctx.blockItem(ctx.blockItem().size() - 1).stmt().RETURN()).getSymbol().getLine() + ":Type mismatched for return type.");
                    }
                }else{
                    if(currentFuncRetType!=null){
                        hasError = true;
                        System.err.println("Error type 7 at Line " + (ctx.blockItem(ctx.blockItem().size() - 1).stmt().RETURN()).getSymbol().getLine() + ":Type mismatched for return type.");
                    }
                }
            }
            return null;
        }
        LocalScope scope = new LocalScope(localScopeCnt++);
        scope.setParent(currentScope);
        currentScope = scope;
        super.visitBlock(ctx);
        currentScope = currentScope.getParent();
        return null;
    }

    private void declHandler(TerminalNode ident, List<SysYParser.ConstExpContext> constExpContexts) {
        String name = ident.getText();
        if(isDeclaredAtSameScope(name)){
            System.err.println("Error type 3 at Line "+ident.getSymbol().getLine()+":Redefined variable:"+name);
            hasError = true;
            return;
        }
        BaseType type;
        if(constExpContexts.size()==0) {
            type = new PrimaryType();
        }else{
            type = new ArrayType();
            for (SysYParser.ConstExpContext a: constExpContexts
            ) {
                ((ArrayType) type).addDimension(Integer.parseInt(a.exp().getText()));
            }
        }
        Symbol symbol = new Symbol(name, type, currentScope);
        symbolTable.put(formatName(name), symbol);
    }
    //rename:

    @Override
    public Void visitTerminal(TerminalNode node) {
        if(node.getSymbol().getType()== SysYLexer.IDENT){
            if(node.getSymbol().getLine() == lineNo && node.getSymbol().getCharPositionInLine() == column){
                toRename = getSymbol(node.getSymbol().getText());
            }
            Symbol s = getSymbol(node.getSymbol().getText());
            if(s!=null){
                s.used.add(node.getSymbol().getLine()+"_"+node.getSymbol().getCharPositionInLine());
            }
        }
        return super.visitTerminal(node);
    }
}
