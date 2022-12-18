public class BaseScope {
    private BaseScope parent;

    public BaseScope getParent() {
        return parent;
    }

    public void setParent(BaseScope parent) {
        this.parent = parent;
    }
}
