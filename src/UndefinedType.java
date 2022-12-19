public class UndefinedType extends BaseType{
    //do nothing

    @Override
    public boolean equals(BaseType type) {
        return type instanceof UndefinedType;
    }
}
