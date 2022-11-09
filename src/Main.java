import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.*;
import java.util.List;

public class Main
{
    public static void main(String[] args) throws IOException {
//        if (args.length < 1) {
//            System.err.println("input path is required");
//        }
        //String source = args[0];
        String source = "E:\\bianyiyuanli\\Lab\\src\\a.txt";
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        sysYLexer.removeErrorListeners();
        MyErrorListener myErrorListener = new MyErrorListener();
        sysYLexer.addErrorListener(myErrorListener);
        List<? extends Token> tokens = sysYLexer.getAllTokens();
        String[] ruleNames = sysYLexer.getRuleNames();
        if(!myErrorListener.hasError) {
            for (Token token : tokens) {
                System.err.println(ruleNames[token.getType() - 1] + " " + token.getText() + " " + "at" + " " + "Line" + " " + token.getLine() + ".");
            }
        }
    }
}
