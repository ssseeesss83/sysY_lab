public class LocalScope extends BaseScope{
    private int no;

    public LocalScope(int no) {
        this.no = no;
    }

    @Override
    public String toString() {
        return "LocalScope_"+no;
    }
}
