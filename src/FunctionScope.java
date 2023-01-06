public class FunctionScope extends BaseScope{
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
