```kotlin
import io.cratis.chronicle.artifacts.IClientArtifacts
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.readModels.ReadModel
import kotlin.reflect.KClass

@EventType
data class StructuralDepsBookBorrowed(val bookId: String = "")

@EventType
data class StructuralDepsBookReturned(val bookId: String = "")

@ReadModel
data class StructuralDepsBorrowedBook(val bookId: String = "")

class StructuralDepsBorrowedBooksProjection : IProjectionFor<StructuralDepsBorrowedBook> {
    override fun define(builder: IProjectionBuilderFor<StructuralDepsBorrowedBook>) {
        builder.from(StructuralDepsBookBorrowed::class) {
            it.set(StructuralDepsBorrowedBook::bookId).to { e -> e.bookId }
        }
    }
}

class StructuralDepsMyArtifacts : IClientArtifacts {
    override val eventTypes: List<KClass<*>> = listOf(StructuralDepsBookBorrowed::class, StructuralDepsBookReturned::class)
    override val eventTypeMigrations: List<KClass<*>> = emptyList()
    override val readModels: List<KClass<*>> = listOf(StructuralDepsBorrowedBook::class)
    override val projections: List<KClass<*>> = listOf(StructuralDepsBorrowedBooksProjection::class)
    override val modelBoundProjections: List<KClass<*>> = emptyList()
    override val reactors: List<KClass<*>> = emptyList()
    override val reducers: List<KClass<*>> = emptyList()
    override val constraints: List<KClass<*>> = emptyList()
    override val eventSeeders: List<KClass<*>> = emptyList()
    override val webhooks: List<KClass<*>> = emptyList()
    override val captures: List<KClass<*>> = emptyList()
    override val reactorMiddlewares: List<KClass<*>> = emptyList()
    override val reactorArgumentResolvers: List<KClass<*>> = emptyList()
}
```
