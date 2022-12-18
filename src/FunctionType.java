import java.util.ArrayList;

public class FunctionType extends BaseType{
    private final BaseType returnType;

    @Override
    public boolean equals(BaseType type) {
        if(type instanceof FunctionType
        &&((FunctionType) type).returnType.equals(this.returnType)
        &&((FunctionType) type).paramTypes.size() == this.paramTypes.size()){
            return true;
        }//else return type instanceof PrimaryType && returnType.equals("int");
        return false;
    }

    public FunctionType(BaseType returnType) {
        this.returnType = returnType;
    }



    private ArrayList<BaseType> paramTypes = new ArrayList<>();

    public BaseType getReturnType() {
        return returnType;
    }

    public ArrayList<BaseType> getParams() {
        return paramTypes;
    }

    public void addParamType(BaseType type){
        paramTypes.add(type);
    }
}
