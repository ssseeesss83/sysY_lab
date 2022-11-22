import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Locale;

public class MyVisitor extends SysYParserBaseVisitor<Void>{

    String[] terminalText = {
    //保留字
    "CONST[orange]",
    "INT[orange]",
    "VOID[orange]",
    "IF[orange]",
    "ELSE[orange]",
    "WHILE[orange]",
    "BREAK[orange]",
    "CONTINUE[orange]",
    "RETURN[orange]",
    //运算符
    "PLUS[blue]",
    "MINUS[blue]",
    "MUL[blue]",
    "DIV[blue]",
    "MOD[blue]",
    "ASSIGN[blue]",
    "EQ[blue]",
    "NEQ[blue]",
    "LT[blue]",
    "GT[blue]",
    "LE[blue]",
    "GE[blue]",
    "NOT[blue]",
    "AND[blue]",
    "OR[blue]",
    //not to use
    "L_PAREN", "R_PAREN", "L_BRACE", "R_BRACE", "L_BRACKT", "R_BRACKT", "COMMA", "SEMICOLON",
    //标识符
    "IDENT[red]",
    //数字与字符串
    "INTEGR_CONST[green]"};

    RuleNode tempParent;

    @Override
    public Void visitChildren(RuleNode node) {
        tempParent = node;
        for(int i = 0; i < node.getRuleContext().depth()-1; i++) System.err.print("  ");
        System.err.println(capitalize(SysYParser.ruleNames[node.getRuleContext().getRuleIndex()]));
        return super.visitChildren(node);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        int depth = tempParent.getRuleContext().depth()-1;
        if(tempParent == node.getParent()){
            depth++;
        }


        int type = node.getSymbol().getType();
        if(type!=-1 && (type <= 24 || type == 33 || type == 34)) {
            for(int i = 0; i < depth; i++) System.err.print("  ");
            System.err.println(node.getSymbol().getText() +" "+ terminalText[type - 1]);
        }

        return super.visitTerminal(node);
    }

    private String capitalize(String s){
        return s.substring(0,1).toUpperCase()+s.substring(1);
    }
}
