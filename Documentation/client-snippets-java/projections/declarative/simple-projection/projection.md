```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

class DecSimpleUserProjection implements IProjectionFor<DecSimpleUser> {
    @Override
    public void define(IProjectionBuilderFor<DecSimpleUser> builder) {
        builder.from(DecSimpleUserCreated.class);
    }
}
```
