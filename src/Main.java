import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.*;
import java.util.List;

public class Main
{
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        //String source = "E:\\bianyiyuanli\\Lab\\src\\a.txt";
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        String[] ruleNames = sysYLexer.getRuleNames();
        sysYLexer.removeErrorListeners();
        MyErrorListener myErrorListener = new MyErrorListener();
        sysYLexer.addErrorListener(myErrorListener);
        List<? extends Token> tokens = sysYLexer.getAllTokens();
        if(!myErrorListener.hasError) {
            for (Token token : tokens) {
                String text = token.getText();
                if(token.getType() == SysYLexer.INTEGR_CONST){
                    text = toDemical(text);
                }
                System.err.println(ruleNames[token.getType() - 1] + " " + text + " " + "at" + " " + "Line" + " " + token.getLine() + ".");
            }
        }
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
