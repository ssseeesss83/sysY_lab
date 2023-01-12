import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMVisitor extends SysYParserBaseVisitor<LLVMValueRef>{
    private BaseScope currentScope;
    private int localScopeCnt;
    boolean isFunctionBlock = false; //用于遍历标志是否是函数的括号作用域
    LLVMBasicBlockRef currentBlock = null;
    LLVMValueRef currentFunction = null;
    LLVMBasicBlockRef currentEntry = null;
    boolean visitFunctionBlock = true;
    private Map<String, LLVMValueRef> symbolTable = new HashMap<>();


    //创建module
    LLVMModuleRef module = LLVMModuleCreateWithName("moudle");
    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    LLVMBuilderRef builder = LLVMCreateBuilder();
    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    LLVMTypeRef i32Type = LLVMInt32Type();
    LLVMTypeRef voidType = LLVMVoidType();
    LLVMTypeRef i1Type  = LLVMInt1Type();
    //创建一个常量,这里是常数0
    LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
    LLVMValueRef _true = LLVMConstInt(i32Type, 1, 0);
    LLVMValueRef _false = LLVMConstInt(i32Type, 0, 0);

    LLVMValueRef result;

    private boolean isDeclaredAtSameScope(String name){
        return symbolTable.containsKey(formatName(name));
    }

    private String formatName(String name){
        return currentScope.toString()+"_"+name;
    }

    private String formatName(String name, BaseScope scope){
        return scope.toString()+"_"+name;
    }

    //this is nullable.
    private LLVMValueRef getSymbol(String name){
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


    public LLVMVisitor() {
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        isFunctionBlock = true;
        String name = ctx.IDENT().getText();
        FunctionScope scope = new FunctionScope(name);
        scope.setParent(currentScope);
        currentScope = scope;
        //生成返回值类型
        LLVMTypeRef returnType = ctx.funcType().getText().equals("void")?voidType:i32Type;
        //参数类型
        PointerPointer<Pointer> argTypes;
        int i = 0; //argCount
        if(ctx.funcFParams()!=null) {
            argTypes = new PointerPointer<>(ctx.funcFParams().funcFParam().size());
            if (ctx.funcFParams() != null) {
                for (SysYParser.FuncFParamContext param : ctx.funcFParams().funcFParam() //遍历形参
                ) {
                    argTypes.put(i++, i32Type);
                }
            }
        }else{
            argTypes = new PointerPointer<>(0);
        }
        LLVMTypeRef ft = LLVMFunctionType(returnType, argTypes, /* argumentCount */ i, /* isVariadic */ 0);
        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/name, ft);
        currentBlock = LLVMAppendBasicBlock(function, /*blockName:String*/name+"Entry");
        LLVMPositionBuilderAtEnd(builder,currentBlock);
        for(int j = 0; j < i; j++){
            String paramName = ctx.funcFParams().funcFParam().get(j).IDENT().getText();
            LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, formatName(paramName)+"_Pointer");
            LLVMBuildStore(builder, LLVMGetParam(function, /* parameterIndex */j), pointer);
            symbolTable.put(formatName(paramName),pointer);
        }

        symbolTable.put(formatName(name, currentScope.getParent()),function);
        currentFunction = function;
        currentEntry = currentBlock;
        super.visitFuncDef(ctx);

        isFunctionBlock = false;
        currentScope = currentScope.getParent();
        currentBlock=null;
        return function;
    }

    @Override
    public LLVMValueRef visitCompUnit(SysYParser.CompUnitContext ctx) {
        currentScope = new GlobalScope();
        return super.visitCompUnit(ctx);
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if(ctx.lVal()!=null){//赋值语句
            super.visitStmt(ctx);
            LLVMValueRef lval = getSymbol(ctx.lVal().IDENT().getText());
            //System.out.println(LLVMTypeOf(lval)==i32Type);
            if(LLVMTypeOf(lval)==i32Type) {
                LLVMBuildStore(builder,
                        getExpVal(ctx.exp()),
                        lval);
            }else{//array
                if(ctx.lVal().L_BRACKT().size()==0){
                    LLVMBuildStore(builder,
                            getExpVal(ctx.exp()),
                            lval);
                   // System.out.println(LLVMConstIntGetSExtValue(LLVMBuildLoad(builder,lval,"")));
                }else {
                    LLVMBuildStore(builder,
                            getExpVal(ctx.exp()),
                            LLVMBuildGEP(builder, lval, new PointerPointer<>(zero, getExpVal(ctx.lVal().exp(0))), 2, "array_pointer")
                    );
                }
            }
        }else if(ctx.exp() instanceof SysYParser.CallFuncExpContext && ctx.RETURN()==null){//函数调用语句
            super.visitStmt(ctx);
            SysYParser.ExpContext exp = ctx.exp();
            return functionCallHandler((SysYParser.CallFuncExpContext) exp);
        }else if(ctx.IF()!=null){//条件语句
            LLVMValueRef cond = LLVMBuildICmp(builder,LLVMIntNE, getCond(ctx.cond()),zero,"cond");
            //System.out.println(LLVMTypeOf(cond));
            LLVMBasicBlockRef ifTrue = LLVMAppendBasicBlock(currentFunction,"if_true");
            LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(currentFunction,"if_false");
            LLVMBasicBlockRef entry = LLVMAppendBasicBlock(currentFunction,"entry");
            LLVMBuildCondBr(builder, cond, ifTrue, ifFalse);
            currentEntry = entry;
            currentBlock = ifTrue;
            LLVMPositionBuilderAtEnd(builder,ifTrue);
            visitBlock(ctx.stmt(0).block());
            LLVMBuildBr(builder,entry);
            currentBlock = ifFalse;
            LLVMPositionBuilderAtEnd(builder,ifFalse);
            if(ctx.ELSE()!=null){
                visitBlock(ctx.stmt(1).block());
            }
            LLVMBuildBr(builder,entry);
            LLVMPositionBuilderAtEnd(builder,entry);
        }
        return null;
    }



    private LLVMValueRef functionCallHandler(SysYParser.CallFuncExpContext exp) {
        PointerPointer<LLVMValueRef> rParams = new PointerPointer<>();
        int size = 0;
        if(exp.funcRParams()!=null) {
            rParams = new PointerPointer<>(exp.funcRParams().param().size());
            for (int i = 0; i < exp.funcRParams().param().size(); i++) {
                rParams.put(i, getExpVal(exp.funcRParams().param().get(i).exp()));
            }
            size = exp.funcRParams().param().size();
        }
        return LLVMBuildCall(builder,getSymbol(exp.IDENT().getText()),
                rParams,
                size,"");
    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        List<SysYParser.ConstDefContext> constDefs = ctx.constDef();
        for (SysYParser.ConstDefContext constDef:constDefs
        ) {
            declHandler(constDef.IDENT(), constDef.constExp(), constDef.constInitVal());
        }
        return super.visitConstDecl(ctx);
    }

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        List<SysYParser.VarDefContext> varDefs = ctx.varDef();

        for (SysYParser.VarDefContext varDef:varDefs
        ) {
            declHandler(varDef.IDENT(), varDef.constExp(), varDef.initVal());
        }
        return super.visitVarDecl(ctx);
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        if(isFunctionBlock&&currentBlock!=null){
            isFunctionBlock = false;
            //选择要在哪个基本块后追加指令
            LLVMPositionBuilderAtEnd(builder, currentBlock);//后续生成的指令将追加在block1的后面
            super.visitBlock(ctx);
            for (SysYParser.BlockItemContext blockItem: ctx.blockItem()
                 ) {
                if(blockItem.stmt()!=null && blockItem.stmt().RETURN()!=null) {
                    SysYParser.ExpContext exp = blockItem.stmt().exp();
                    if(exp!=null){
                        if(!(exp instanceof SysYParser.CallFuncExpContext)) {
                            LLVMBuildRet(builder, getExpVal(exp));
                        }else{
                            getExpVal(exp);
                            LLVMBuildRetVoid(builder);
                        }
                        return null;
                    }
                }
            }
            return LLVMBuildRetVoid(builder);
        }
        LocalScope scope = new LocalScope(localScopeCnt++);
        scope.setParent(currentScope);
        currentScope = scope;
      //  LLVMPositionBuilderAtEnd(builder,currentBlock);
        super.visitBlock(ctx);
        currentScope = currentScope.getParent();
        return null;
    }

    LLVMValueRef getExpVal(SysYParser.ExpContext exp){
        SysYParser.ExpContext lexp;
        SysYParser.ExpContext rexp;
        String op;

        if(exp instanceof SysYParser.MulExpContext){
            lexp=((SysYParser.MulExpContext) exp).exp(0);
            rexp=((SysYParser.MulExpContext) exp).exp(1);
            op = exp.getChild(1).getText();
            switch (op){
                case "*":
                    return LLVMBuildMul(builder,getExpVal(lexp),getExpVal(rexp),"temp");
                case "/":
                    return LLVMBuildSDiv(builder,getExpVal(lexp),getExpVal(rexp),"temp");
                case "%":
                    return LLVMBuildSRem(builder,getExpVal(lexp),getExpVal(rexp),"temp");
            }
        }else if(exp instanceof SysYParser.PlusExpContext){
            lexp=((SysYParser.PlusExpContext) exp).exp(0);
            rexp=((SysYParser.PlusExpContext) exp).exp(1);
            op = exp.getChild(1).getText();
            switch (op){
                case "+":
                    return LLVMBuildAdd(builder,getExpVal(lexp),getExpVal(rexp),"temp");
                case "-":
                    return LLVMBuildSub(builder,getExpVal(lexp),getExpVal(rexp),"temp");
            }
        }else if(exp instanceof SysYParser.UnaryOpExpContext){
            lexp=((SysYParser.UnaryOpExpContext) exp).exp();
            op=((SysYParser.UnaryOpExpContext) exp).unaryOp().getChild(0).getText();
            switch (op){
                case "-":
                    return LLVMBuildNeg(builder, getExpVal(lexp),"temp");
                case "!":
                    LLVMValueRef cmp = LLVMBuildICmp(builder, LLVMIntEQ, getExpVal(lexp), zero, "temp");
                    return LLVMBuildSelect(builder, cmp, LLVMConstInt(LLVMInt32Type(), 1, 0), LLVMConstInt(LLVMInt32Type(), 0, 0),"temp");
                case "+":
                    return getExpVal(lexp);
            }
        }else if(exp instanceof SysYParser.NumberExpContext){
            return LLVMConstInt(i32Type, Integer.parseInt(((SysYParser.NumberExpContext) exp).number().getText()),0);
        }else if(exp instanceof SysYParser.ExpParenthesisContext){
            return getExpVal(((SysYParser.ExpParenthesisContext) exp).exp());
        }else if(exp instanceof SysYParser.LvalExpContext){
            //return LLVMBuildLoad(builder, ref, "");
            LLVMValueRef ref = getSymbol(((SysYParser.LvalExpContext) exp).lVal().IDENT().getText());
            LLVMValueRef val;
            if(LLVMTypeOf(ref)==i32Type) {
                val = LLVMBuildLoad(builder, ref, "");
            }else{//arrayType
                if(((SysYParser.LvalExpContext) exp).lVal().L_BRACKT().size()==0){
                    val = LLVMBuildLoad(builder, ref, ""); //pointer
                }else{
                    LLVMValueRef index = getExpVal(((SysYParser.LvalExpContext) exp).lVal().exp().get(0));
                    val = LLVMBuildLoad(builder,
                            LLVMBuildGEP(builder, ref, new PointerPointer<LLVMValueRef>(zero, index), 2, ""),
                            "");
                }
            }
            return val;
        }else if(exp instanceof SysYParser.CallFuncExpContext){
            return functionCallHandler((SysYParser.CallFuncExpContext) exp);
        }
        return zero;
    }

    LLVMValueRef getCond(SysYParser.CondContext cond){
        LLVMValueRef res = zero;
        if(cond instanceof SysYParser.ExpCondContext){
         //   System.out.println(((SysYParser.ExpCondContext) cond).exp().getText());
            return getExpVal(((SysYParser.ExpCondContext) cond).exp());
        }else if(cond instanceof SysYParser.LtCondContext){
            switch (cond.getChild(1).getText()){
                case "<":
                    res = LLVMBuildICmp(builder,LLVMIntSLT,
                            getCond(((SysYParser.LtCondContext) cond).cond(0)),
                            getCond(((SysYParser.LtCondContext) cond).cond(1)),"<");
                    break;
                case ">":
                    res = LLVMBuildICmp(builder,LLVMIntSGT,
                            getCond(((SysYParser.LtCondContext) cond).cond(0)),
                            getCond(((SysYParser.LtCondContext) cond).cond(1)),">");
                    break;
                case "<=":
                    res = LLVMBuildICmp(builder,LLVMIntSLE,
                            getCond(((SysYParser.LtCondContext) cond).cond(0)),
                            getCond(((SysYParser.LtCondContext) cond).cond(1)),"<=");
                    break;
                case ">=":
                    res = LLVMBuildICmp(builder,LLVMIntSGE,
                            getCond(((SysYParser.LtCondContext) cond).cond(0)),
                            getCond(((SysYParser.LtCondContext) cond).cond(1)),">=");
                    break;
            }
        }else if(cond instanceof SysYParser.EqCondContext){
            switch (cond.getChild(1).getText()){
                case "==":
                    res = LLVMBuildICmp(builder,LLVMIntEQ,
                            getCond(((SysYParser.EqCondContext) cond).cond(0)),
                            getCond(((SysYParser.EqCondContext) cond).cond(1)),"==");
                    break;
                case "!=":
                    res = LLVMBuildICmp(builder,LLVMIntNE,
                            getCond(((SysYParser.EqCondContext) cond).cond(0)),
                            getCond(((SysYParser.EqCondContext) cond).cond(1)),"!=");
                    break;
            }
        }else if(cond instanceof SysYParser.AndCondContext){
            return LLVMBuildAnd(builder, getCond(((SysYParser.AndCondContext) cond).cond(0)),
                    getCond(((SysYParser.AndCondContext) cond).cond(1)),"||");
        }else if(cond instanceof SysYParser.OrCondContext){
            return LLVMBuildOr(builder, getCond(((SysYParser.OrCondContext) cond).cond(0)),
                    getCond(((SysYParser.OrCondContext) cond).cond(1)),"||");
        }
        return LLVMBuildZExt(builder,res, i32Type,"i1_to_i32");
    }
    private void declHandler(TerminalNode ident, List<SysYParser.ConstExpContext> constExpContexts, SysYParser.InitValContext initVal) {
        String name = ident.getText();
        LLVMTypeRef type;
        LLVMValueRef ref;
        if(constExpContexts.size()==0) {
            type = i32Type;
            if(currentScope instanceof GlobalScope){
                ref = LLVMAddGlobal(module,i32Type,"global_"+name);
            }else {
                ref = LLVMBuildAlloca(builder, i32Type, formatName(name));
            }
            if (initVal != null) {
                if (initVal.initVal().size() != 0 && initVal.initVal().get(0).exp() != null) {
                    if(currentScope instanceof GlobalScope){
                        LLVMSetInitializer(ref,getExpVal(initVal.initVal().get(0).exp()));
                    }else {
                        LLVMBuildStore(builder, getExpVal(initVal.initVal().get(0).exp()), ref);
                    }
                } else if (initVal.exp() != null) {
                    if(currentScope instanceof GlobalScope){
                        LLVMSetInitializer(ref,getExpVal(initVal.exp()));
                    }else {
                        LLVMBuildStore(builder, getExpVal(initVal.exp()), ref);
                    }
                }
            }else if(currentScope instanceof GlobalScope){
                LLVMSetInitializer(ref,zero);//默认初始化为0
            }
        }else{
            int size = Integer.parseInt (((SysYParser.NumberExpContext) constExpContexts.get(0).exp()).number().INTEGR_CONST().getText());
            List<SysYParser.InitValContext> initValContexts = new ArrayList<>();
            if(initVal!=null) {
                initValContexts = initVal.initVal();
            }
            type = LLVMArrayType(i32Type, size);
            if(currentScope instanceof GlobalScope){
                ref = LLVMAddGlobal(module, type, "global_array_"+name);
                PointerPointer<LLVMValueRef> elements = new PointerPointer<>(size);
                for(int i = 0; i < size; i ++){
                    if(i<initValContexts.size()) {
                        elements.put(i,getExpVal(initValContexts.get(i).exp()));
                    }else{
                        elements.put(i,zero);
                    }
                }
                LLVMSetInitializer(ref,LLVMConstArray(i32Type,elements,size));
            }else {
                LLVMValueRef array = LLVMBuildAlloca(builder, type, formatName(name));
                ref = array;
                for(int i = 0; i < size; i ++){
                        PointerPointer<LLVMValueRef> indices = new PointerPointer<>(zero, LLVMConstInt(i32Type, i, 0));
                        if(i<initValContexts.size()) {
                            //int init_i = Integer.parseInt(((SysYParser.NumberExpContext) initValContexts.get(i).exp()).number().INTEGR_CONST().getText());
                            LLVMBuildStore(builder,
                                    getExpVal(initValContexts.get(i).exp()),
                                    LLVMBuildGEP(builder, array, indices, 2, "GEP_")
                                    );
                        }else{
                            LLVMBuildStore(builder,
                                    zero,
                                    LLVMBuildGEP(builder, array, indices, 2, "GEP_")
                            );
                        }
                }
            }
        }
        symbolTable.put(formatName(name), ref);
    }
    private void declHandler(TerminalNode ident, List<SysYParser.ConstExpContext> constExpContexts, SysYParser.ConstInitValContext initVal) {
        String name = ident.getText();
        LLVMTypeRef type;
        LLVMValueRef ref;
        if(constExpContexts.size()==0) {
            type = i32Type;
            if(currentScope instanceof GlobalScope){
                ref = LLVMAddGlobal(module,i32Type,"const_global_");
            }else {
                ref = LLVMBuildAlloca(builder, i32Type, formatName(name));
            }
            if (initVal != null) {
                if (initVal.constInitVal().size() != 0 && initVal.constInitVal().get(0).constExp().exp()!= null) {
                    if(currentScope instanceof GlobalScope){
                        LLVMSetInitializer(ref,getExpVal(initVal.constInitVal().get(0).constExp().exp()));
                    }else {
                        LLVMBuildStore(builder, getExpVal(initVal.constInitVal().get(0).constExp().exp()), ref);
                    }
                } else if (initVal.constExp().exp() != null) {
                    if(currentScope instanceof GlobalScope){
                        LLVMSetInitializer(ref,getExpVal(initVal.constExp().exp()));
                    }else {
                        LLVMBuildStore(builder, getExpVal(initVal.constExp().exp()), ref);
                    }
                }
            }else if(currentScope instanceof GlobalScope){
                LLVMSetInitializer(ref,zero);//默认初始化为0
            }
        }else{
            int size = Integer.parseInt (((SysYParser.NumberExpContext) constExpContexts.get(0).exp()).number().INTEGR_CONST().getText());
            List<SysYParser.ConstInitValContext> initValContexts = initVal.constInitVal();
            type = LLVMArrayType(i32Type, size);
            if(currentScope instanceof GlobalScope){
                ref = LLVMAddGlobal(module, type, "const_global_array_");
                PointerPointer<LLVMValueRef> elements = new PointerPointer<>(size);
                for(int i = 0; i < size; i ++){
                    if(i<initValContexts.size()) {
                        elements.put(i,getExpVal(initValContexts.get(i).constExp().exp()));
                    }else{
                        elements.put(i,zero);
                    }
                }
                LLVMSetInitializer(ref,LLVMConstArray(i32Type,elements,size));
            }else {
                LLVMValueRef array = LLVMBuildAlloca(builder, type, formatName(name));
                ref = array;
                for(int i = 0; i < size; i ++){
                    PointerPointer<LLVMValueRef> indices = new PointerPointer<>(zero, LLVMConstInt(i32Type, i, 0));
                    if(i<initValContexts.size()) {
                        //int init_i = Integer.parseInt(((SysYParser.NumberExpContext) initValContexts.get(i).exp()).number().INTEGR_CONST().getText());
                        LLVMBuildStore(builder,
                                getExpVal(initValContexts.get(i).constExp().exp()),
                                LLVMBuildGEP(builder, array, indices, 2, "GEP_")
                        );
                    }else{
                        LLVMBuildStore(builder,
                                zero,
                                LLVMBuildGEP(builder, array, indices, 2, "GEP_")
                        );
                    }
                }
            }
        }
        symbolTable.put(formatName(name), ref);
    }

}
