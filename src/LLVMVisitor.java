import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMVisitor extends SysYParserBaseVisitor<LLVMValueRef>{
    //创建module
    LLVMModuleRef module = LLVMModuleCreateWithName("moudle");
    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    LLVMBuilderRef builder = LLVMCreateBuilder();
    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    LLVMTypeRef i32Type = LLVMInt32Type();
    //创建一个常量,这里是常数0
    LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);

    LLVMValueRef result;


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
        //生成返回值类型
        LLVMTypeRef returnType = i32Type;
        LLVMTypeRef   ft = LLVMFunctionType(returnType, i32Type, /* argumentCount */ 0, /* isVariadic */ 0);
        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/"main", ft);
        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/"mainEntry");
        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block1);//后续生成的指令将追加在block1的后面


        //函数返回指令



        SysYParser.BlockContext functionBlock = ctx.block();
        SysYParser.ExpContext exp = functionBlock.blockItem(0).stmt().exp();
        LLVMBuildRet(builder, /*result:LLVMValueRef*/LLVMConstInt(i32Type, getExpRef(exp), /* signExtend */ 0));

        super.visitFuncDef(ctx);
        return function;
    }

    int getExpRef(SysYParser.ExpContext exp){
        SysYParser.ExpContext lexp;
        SysYParser.ExpContext rexp;
        String op;

        if(exp instanceof SysYParser.MulExpContext){
            lexp=((SysYParser.MulExpContext) exp).exp(0);
            rexp=((SysYParser.MulExpContext) exp).exp(1);
            op = exp.getChild(1).getText();
            switch (op){
                case "*":
                    return getExpRef(lexp)*getExpRef(rexp);
                case "/":
                    return getExpRef(lexp)/getExpRef(rexp);
                case "%":
                    return getExpRef(lexp)%getExpRef(rexp);
            }
        }else if(exp instanceof SysYParser.PlusExpContext){
            lexp=((SysYParser.PlusExpContext) exp).exp(0);
            rexp=((SysYParser.PlusExpContext) exp).exp(1);
            op = exp.getChild(1).getText();
            switch (op){
                case "+":
                    return getExpRef(lexp)+getExpRef(rexp);
                case "-":
                    return getExpRef(lexp)-getExpRef(rexp);
            }
        }else if(exp instanceof SysYParser.UnaryOpExpContext){
            lexp=((SysYParser.UnaryOpExpContext) exp).exp();
            op=((SysYParser.UnaryOpExpContext) exp).unaryOp().getChild(0).getText();
            switch (op){
                case "-":
                    return -getExpRef(lexp);
                case "!":
                    return getExpRef(lexp)==0?0:1;
                case "+":
                    return getExpRef(lexp);
            }
        }else if(exp instanceof SysYParser.NumberExpContext){
            return Integer.parseInt(Main.toDemical(((SysYParser.NumberExpContext) exp).number().getText()));
        }else if(exp instanceof SysYParser.ExpParenthesisContext){
            return getExpRef(((SysYParser.ExpParenthesisContext) exp).exp());
        }
        return 0;
    }

}
