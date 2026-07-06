```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import kotlin.jvm.JvmClassMappingKt;

@EventType(id = "index-automap-account-opened")
record IndexAutoMapAccountOpened(String name, double balance) {}

class IndexAutoMapAccountInfo {
    public String name = "";
    public double balance = 0;
}

class IndexAutoMapAccountProjection implements IProjectionFor<IndexAutoMapAccountInfo> {
    @Override
    public void define(IProjectionBuilderFor<IndexAutoMapAccountInfo> builder) {
        // No configure block — matching properties are mapped automatically.
        builder.from(JvmClassMappingKt.getKotlinClass(IndexAutoMapAccountOpened.class), null);
    }
}
```
