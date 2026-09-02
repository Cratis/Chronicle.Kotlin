```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record IndexExplicitAccountInfo(String name, double balance) {
}

class IndexExplicitAccountProjection implements IProjectionFor<IndexExplicitAccountInfo> {
    @Override
    public void define(IProjectionBuilderFor<IndexExplicitAccountInfo> builder) {
        builder.from(IndexExplicitAccountOpened.class, from -> {
            from.set("name").toProperty("name");
            from.set("balance").toProperty("initialBalance");
        });
    }
}
```
