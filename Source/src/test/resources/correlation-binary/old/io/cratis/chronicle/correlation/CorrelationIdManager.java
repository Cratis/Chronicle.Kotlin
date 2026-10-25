package io.cratis.chronicle.correlation;

import java.util.UUID;

/** Frozen pre-extraction signatures; compile-time only, never on the runtime classpath. */
public final class CorrelationIdManager {
    public CorrelationIdManager() { }
    public final UUID getCurrent() { return null; }
    public final void set(UUID id) { }
    public final void clear() { }
}
