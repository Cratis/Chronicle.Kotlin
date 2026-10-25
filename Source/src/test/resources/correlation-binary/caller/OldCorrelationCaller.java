import io.cratis.chronicle.correlation.CorrelationIdManager;
import io.cratis.chronicle.correlation.CorrelationIdManagerKt;
import java.util.UUID;

public final class OldCorrelationCaller {
    private static final UUID ZERO = new UUID(0L, 0L);

    private static void expect(boolean condition, String name) {
        if (!condition) throw new IllegalStateException(name);
    }

    public static String run() {
        var singleton = CorrelationIdManagerKt.getCorrelationIdManager();
        expect(singleton == CorrelationIdManagerKt.getCorrelationIdManager(), "singleton identity");
        var manager = new CorrelationIdManager();
        expect(manager != singleton, "isolation");
        var first = manager.getCurrent();
        expect(first != null && first.equals(manager.getCurrent()), "lazy stable");
        var id = UUID.randomUUID();
        manager.set(id);
        expect(id.equals(manager.getCurrent()), "set");
        manager.set(ZERO);
        expect(ZERO.equals(manager.getCurrent()), "zero");
        manager.clear();
        expect(!ZERO.equals(manager.getCurrent()) && !id.equals(manager.getCurrent()), "clear");
        return "ok";
    }
}
