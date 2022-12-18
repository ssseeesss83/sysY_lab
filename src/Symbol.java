import java.util.ArrayList;
import java.util.Objects;

public class Symbol {
    private final String name;


    private final BaseType type;
    private final BaseScope scope;
    public ArrayList<String> used = new ArrayList<>();

    public Symbol(String name, BaseType type, BaseScope scope) {
        this.name = name;
        this.type = type;
        this.scope = scope;
    }


    public String getName() {
        return name;
    }

    public BaseType getType() {
        return type;
    }

    public BaseScope getScope() {
        return scope;
    }

}
