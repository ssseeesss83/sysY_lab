import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;


public class MyVisitor extends SysYParserBaseVisitor<Void>{
    private Symbol symbol;
    private String name;
    MyVisitor(Symbol symbol, String name){
        this.symbol = symbol;
        this.name = name;
    }

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


    @Override
    public Void visitChildren(RuleNode node) {
        for(int i = 0; i < node.getRuleContext().depth()-1; i++) System.err.print("  ");
        System.err.println(capitalize(SysYParser.ruleNames[node.getRuleContext().getRuleIndex()]));
        return super.visitChildren(node);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        int depth = 0;
        ParseTree tempParent = node.getParent();
        while(tempParent!=null){
            tempParent = tempParent.getParent();
            depth++;
        }
        int type = node.getSymbol().getType();
        if(type!=-1 && (type <= 24 || type == 33 || type == 34)) {
            for(int i = 0; i < depth; i++) System.err.print("  ");

            if(node.getSymbol().getType() == SysYLexer.INTEGR_CONST){
                System.err.println(Main.toDemical(node.getSymbol().getText()) + " " + terminalText[type - 1]);
            }else if(node.getSymbol().getType()== SysYLexer.IDENT){
                String where = node.getSymbol().getLine()+"_"+node.getSymbol().getCharPositionInLine();
                if(symbol.used.contains(where)){
                    System.err.println(name + " " + terminalText[type - 1]);
                }else{
                    System.err.println(node.getSymbol().getText() + " " + terminalText[type - 1]);
                }
            }else {
                System.err.println(node.getSymbol().getText() + " " + terminalText[type - 1]);
            }
        }

        return super.visitTerminal(node);
    }

    private String capitalize(String s){
        return s.substring(0,1).toUpperCase()+s.substring(1);
    }
}
