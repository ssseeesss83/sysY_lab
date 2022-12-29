import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import java.io.*;


public class Main
{
    public static final BytePointer error = new BytePointer();

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
//        int lineNo = 12;
//        int column = 4;
//        String name = "XXX";
//        String source = "E:\\bianyiyuanli\\Lab\\src\\a.txt";
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        //String[] ruleNames = sysYLexer.getRuleNames();
//        sysYLexer.removeErrorListeners();
        MyErrorListener myErrorListener = new MyErrorListener();
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        sysYParser.removeErrorListeners();
        sysYParser.addErrorListener(myErrorListener);
        ParseTree tree = sysYParser.program();
        LLVMVisitor llvmVisitor = new LLVMVisitor();
        llvmVisitor.visit(tree);


        if (LLVMPrintModuleToFile(llvmVisitor.module, args[1], error) != 0) {
            LLVMDisposeMessage(error);
        }


//        SymbolVisitor symbolVisitor = new SymbolVisitor();
//        symbolVisitor.lineNo = lineNo;
//        symbolVisitor.column = column;
//        symbolVisitor.argLen = args.length;
//        symbolVisitor.visit(tree);
//        if(!symbolVisitor.hasError){
//            MyVisitor visitor = new MyVisitor(symbolVisitor.toRename, name);
//            visitor.visit(tree);
//        }
//        System.out.println("...");
//        if(!myErrorListener.hasError) {
//            visitor.visit(tree);
//        }
    }

    public static String toDemical(String x){
        int y = 0;
        if(x.startsWith("0x") || x.startsWith("0X")){
            y = Integer.valueOf(x.substring(2) , 16);
        }else if(x.startsWith("0") && x.length()>1){
            y = Integer.valueOf(x.substring(1), 8);
        }else{
            y = Integer.parseInt(x);
        }
        return y+"";
    }
}
