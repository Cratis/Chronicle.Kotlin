```java
import io.cratis.chronicle.artifacts.IClientArtifacts;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.readModels.ReadModel;
import io.cratis.chronicle.java.ProjectionBuilderJavaBridge;

import java.util.Collections;
import java.util.List;

import kotlin.reflect.KClass;
import kotlin.jvm.JvmClassMappingKt;

@EventType
record StructuralDepsBookBorrowed(String bookId) {}

@EventType
record StructuralDepsBookReturned(String bookId) {}

@ReadModel
class StructuralDepsBorrowedBook {
    public String bookId = "";
}

class StructuralDepsBorrowedBooksProjection implements IProjectionFor<StructuralDepsBorrowedBook> {
    @Override
    public void define(IProjectionBuilderFor<StructuralDepsBorrowedBook> builder) {
        // AutoMap matches StructuralDepsBookBorrowed.bookId to StructuralDepsBorrowedBook.bookId by name.
        ProjectionBuilderJavaBridge.from(builder, StructuralDepsBookBorrowed.class);
    }
}

class StructuralDepsMyArtifacts implements IClientArtifacts {
    private static KClass<?> kotlin(Class<?> type) {
        return JvmClassMappingKt.getKotlinClass(type);
    }

    @Override
    public List<KClass<?>> getEventTypes() {
        return List.of(kotlin(StructuralDepsBookBorrowed.class), kotlin(StructuralDepsBookReturned.class));
    }

    @Override
    public List<KClass<?>> getEventTypeMigrations() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getReadModels() { return List.of(kotlin(StructuralDepsBorrowedBook.class)); }

    @Override
    public List<KClass<?>> getProjections() { return List.of(kotlin(StructuralDepsBorrowedBooksProjection.class)); }

    @Override
    public List<KClass<?>> getModelBoundProjections() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getReactors() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getReducers() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getConstraints() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getEventSeeders() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getWebhooks() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getCaptures() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getReactorMiddlewares() { return Collections.emptyList(); }

    @Override
    public List<KClass<?>> getReactorArgumentResolvers() { return Collections.emptyList(); }
}
```
