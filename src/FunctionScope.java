public class FunctionScope extends GlobalScope{
    private String functionName;
    FunctionScope(String name){
        functionName = name;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String toString() {
        return "FunctionScope_" + functionName;
    }
}
