public class PrimaryType extends BaseType{
    @Override
    public boolean equals(BaseType type) {
//        if(type instanceof FunctionType){
//            return ((FunctionType) type).getReturnType().equals("int");
//        }
        return type instanceof PrimaryType;
    }
}
