import java.util.ArrayList;

public class ArrayType extends BaseType{
    private ArrayList<Integer> sizes=new ArrayList<>();

    @Override
    public boolean equals(BaseType type) {
        return (type instanceof ArrayType && ((ArrayType) type).sizes.size()==this.sizes.size());
    }

    public int getDim(){
        return sizes.size();
    }
    //    @Override
//    public boolean equals(BaseType type) {
//        if(type instanceof ArrayType
//        && ((ArrayType) type).sizes.size() == this.sizes.size()){
//            for(int i = 0; i < sizes.size(); i ++){
//                if(((ArrayType) type).getSizes().get(i)!=this.sizes.get(i)){
//                    return false;
//                }
//            }
//            return true;
//        }
//        return false;
//    }

    public void addDimension(int size){
        sizes.add(size);
    }

    public ArrayList<Integer> getSizes() {
        return sizes;
    }
}
