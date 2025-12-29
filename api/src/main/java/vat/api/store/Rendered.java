package vat.api.store;

import lombok.ToString;
import lombok.experimental.Delegate;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Zen.Liu
 * @since 2025-10-22
 */
public @ToString class Rendered implements Renderer {

    @Delegate(types = Renderer.class)
    public final Dialect dialect;
    /// primary query
    public final Writer query = new Writer(this);
    /// parameters
    public final Map<String, Object> parameter = new HashMap<>();
    /// place-holder
    public final Map<String, PlaceHolder> placeHolder = new HashMap<>();

    /// object reader
    @Nullable
    public Reader<?> reader;
    /// current render stage
    @Nullable
    public Stage stage;
    /// parameter for batch executing;
    @Nullable
    public List<Map<String, Object>> parameters;


    public final  @Nullable Stage stage() {
        return stage;
    }

    public Rendered(Dialect dialect) {
        this.dialect = dialect;
    }

    public final Writer render(Writer w, @Nullable Object value) {
        switch (value) {
            case Renderable r -> r._render(this, w);
            case null -> w.w(dialect.valueNull());
            case Boolean b -> w.w(b ? "TRUE" : "FALSE");
            case Integer i when i > -10 && i < 10 -> w.w(i);
            case Long i when i > -10 && i < 10 -> w.w(i.intValue());
            default -> {
                var n = parameter.size();
                var name = "p%d".formatted(n);
                w.p(name);
                parameter.put(name, dialect.parameter(w, value));
            }
        }
        return w;
    }

    @Override
    public final boolean registerPlaceHolder(Writer w, String name, Class<?> type) {
        if (placeHolder.containsKey(name)) {
            return false;
        }
        placeHolder.put(name, new PlaceHolder(name, type));
        w.p(name);
        return true;
    }


}
