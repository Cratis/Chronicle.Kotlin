```kotlin title="Include child projection events"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.time.Instant

@EventType(id = "order-created-declarative-every-children")
data class OrderCreatedDeclarativeEveryChildren(val orderNumber: String)

@EventType(id = "item-added-declarative-every-children")
data class ItemAddedDeclarativeEveryChildren(
    val orderId: String,
    val productId: String,
    val productName: String,
    val quantity: Int
)

@EventType(id = "item-quantity-changed-declarative-every-children")
data class ItemQuantityChangedDeclarativeEveryChildren(
    val orderId: String,
    val productId: String,
    val quantity: Int
)

data class OrderDeclarativeEveryChildren(
    val orderNumber: String = "",
    val lastModified: Instant = Instant.EPOCH,
    val items: List<OrderItemDeclarativeEveryChildren> = emptyList()
)

data class OrderItemDeclarativeEveryChildren(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0
)

class OrderDeclarativeEveryChildrenProjection : IProjectionFor<OrderDeclarativeEveryChildren> {
    override fun define(builder: IProjectionBuilderFor<OrderDeclarativeEveryChildren>) {
        builder
            .from(OrderCreatedDeclarativeEveryChildren::class)
            .fromEvery { it.set(OrderDeclarativeEveryChildren::lastModified).toEventContextProperty("occurred") }
            .children(OrderDeclarativeEveryChildren::items, OrderItemDeclarativeEveryChildren::class) { children ->
                children
                    .identifiedBy("productId")
                    .from(ItemAddedDeclarativeEveryChildren::class) {
                        it.usingKey("productId").usingParentKey("orderId")
                    }
                    .from(ItemQuantityChangedDeclarativeEveryChildren::class) {
                        it.usingKey("productId").usingParentKey("orderId")
                    }
            }
    }
}
```
