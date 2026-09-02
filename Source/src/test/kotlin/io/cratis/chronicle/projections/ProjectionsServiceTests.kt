// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.Passive
import io.cratis.chronicle.readModels.ReadModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// --- FromEvent key resolution ---

@EventType
private data class TicketOpened(val orderId: String, val subject: String)

@ReadModel
@FromEvent(TicketOpened::class)
private data class Ticket(
    @SetFrom("subject", TicketOpened::class)
    val subject: String = ""
)

@EventType
private data class OrderCreatedForKey(val orderNumber: String)

@ReadModel
@FromEvent(OrderCreatedForKey::class, key = "orderNumber")
private data class OrderByNumber(val orderNumber: String = "")

// --- Join ---

@EventType
private data class InvoiceRaised(val customerId: String, val amount: Double)

@EventType
private data class CustomerRegistered(val name: String)

@ReadModel
@FromEvent(InvoiceRaised::class)
private data class InvoiceWithCustomer(
    @SetFrom("customerId", InvoiceRaised::class)
    val customerId: String = "",
    @Join(CustomerRegistered::class, on = "customerId", eventPropertyName = "name")
    val customerName: String = ""
)

// --- ChildrenFrom ---

@EventType
private data class ShoppingCartStarted(val cartId: String)

@EventType
private data class ItemAddedToCart(val cartId: String, val itemId: String, val name: String)

@ReadModel
@FromEvent(ShoppingCartStarted::class)
private data class ShoppingCart(
    @ChildrenFrom(ItemAddedToCart::class, key = "itemId", identifiedBy = "itemId")
    val items: List<CartItem> = emptyList()
)

private data class CartItem(
    val itemId: String = "",
    @SetFrom("name", ItemAddedToCart::class)
    @NoAutoMap
    val name: String = ""
)

// --- Nested ---

@EventType
private class EmployeeHired

@EventType
private data class ContractSigned(val title: String)

@EventType
private class ContractEnded

@FromEvent(ContractSigned::class)
@ClearWith(ContractEnded::class)
private data class EmploymentContract(
    @SetFrom("title", ContractSigned::class)
    @NoAutoMap
    val title: String = ""
)

@ReadModel
@FromEvent(EmployeeHired::class)
private data class Employee(
    @Nested
    val contract: EmploymentContract? = null
)

// --- Counters ---

@EventType
private class PageViewed

@EventType
private class ItemLiked

@EventType
private class ItemUnliked

@EventType
private data class CreditsAdded(val amount: Int)

@EventType
private data class CreditsSpent(val amount: Int)

@ReadModel
@FromEvent(PageViewed::class)
@FromEvent(ItemLiked::class)
@FromEvent(ItemUnliked::class)
@FromEvent(CreditsAdded::class)
@FromEvent(CreditsSpent::class)
private data class PageStats(
    @Count(PageViewed::class)
    val views: Int = 0,
    @Increment(ItemLiked::class)
    @Decrement(ItemUnliked::class)
    val likes: Int = 0,
    @AddFrom(CreditsAdded::class, "amount")
    @SubtractFrom(CreditsSpent::class, "amount")
    val credits: Int = 0
)

@ReadModel
@FromEvent(PageViewed::class)
private data class GlobalPageViews(
    @Count(PageViewed::class, constantKey = "singleton")
    val total: Int = 0
)

// --- FromEvery / FromAll ---

@EventType
private data class WidgetCreated(val name: String)

@EventType
private data class WidgetRenamed(val name: String)

@ReadModel
@FromEvent(WidgetCreated::class)
@FromEvent(WidgetRenamed::class)
private data class WidgetAudit(
    @FromEventSourceId
    val id: String = "",
    @FromEvery(contextProperty = "occurred")
    val lastTouchedAt: String = "",
    @FromAll
    val name: String = ""
)

// --- RemovedWith ---

@EventType
private class SubscriptionStarted

@EventType
private class SubscriptionCancelled

@ReadModel
@FromEvent(SubscriptionStarted::class)
@RemovedWith(SubscriptionCancelled::class)
private data class Subscription(val id: String = "")

// --- NotRewindable ---

@ReadModel
@FromEvent(SubscriptionStarted::class)
@NotRewindable
private data class ForwardOnlyLedger(val id: String = "")

@ReadModel
@FromEvent(SubscriptionStarted::class)
private data class RewindableLedger(val id: String = "")

// --- NoAutoMap ---

@ReadModel
@FromEvent(SubscriptionStarted::class)
@NoAutoMap
private data class ManualOnlyReadModel(val id: String = "")

@ReadModel
@FromEvent(WidgetCreated::class)
private data class WidgetWithGuardedName(
    @SetFrom("name", WidgetCreated::class)
    @NoAutoMap
    val name: String = ""
)

@EventType
private data class TaskAdded(val listId: String, val taskId: String, val title: String)

@ReadModel
@FromEvent(ShoppingCartStarted::class)
private data class TaskList(
    @ChildrenFrom(TaskAdded::class, key = "taskId", identifiedBy = "taskId")
    val tasks: List<CartItem> = emptyList()
)

@ReadModel
@FromEvent(EmployeeHired::class)
private data class EmployeeWithGuardedContract(
    @Nested
    val contract: EmploymentContract? = null
)

// --- SetValue ---

@EventType
private class OrderPlacedForStatus

@EventType
private class OrderCancelledForStatus

@ReadModel
@FromEvent(OrderPlacedForStatus::class)
@FromEvent(OrderCancelledForStatus::class)
private data class OrderStatus(
    @SetValue(OrderPlacedForStatus::class, value = "active")
    @SetValue(OrderCancelledForStatus::class, value = "cancelled")
    val status: String = ""
)

@EventType
private data class NoteAdded(val note: String)

@EventType
private class NoteCleared

@ReadModel
@FromEvent(NoteAdded::class)
@FromEvent(NoteCleared::class)
private data class NotableThing(
    @SetFrom("note", NoteAdded::class)
    @SetValue(NoteCleared::class, clear = true)
    val note: String? = null
)

// --- SetFromContext ---

@EventType
private data class OrderPlacedForAudit(val customerName: String)

@ReadModel
@FromEvent(OrderPlacedForAudit::class)
private data class AuditedOrder(
    @SetFrom("customerName", OrderPlacedForAudit::class)
    val customerName: String = "",
    @SetFromContext(OrderPlacedForAudit::class, contextProperty = "occurred")
    val orderedAt: String = ""
)

@ReadModel
@FromEvent(OrderPlacedForAudit::class)
private data class AuditedOrderDefaultContext(
    @SetFromContext(OrderPlacedForAudit::class)
    val occurred: String = ""
)

@ReadModel
@FromEvent(OrderPlacedForAudit::class)
private data class BadContextReadModel(
    @SetFromContext(OrderPlacedForAudit::class, contextProperty = "occured")
    val occured: String = ""
)

// --- Passive ---

@ReadModel
@FromEvent(SubscriptionStarted::class)
@Passive
private data class PassiveSnapshot(val id: String = "")

// --- Property validation ---

@ReadModel
@FromEvent(WidgetCreated::class)
private data class TypoReadModel(
    @SetFrom("nam", WidgetCreated::class)
    val name: String = ""
)

@ReadModel
@FromEvent(ItemAddedToCart::class)
private data class TypoJoinReadModel(
    @Join(CustomerRegistered::class, on = "customerId", eventPropertyName = "naem")
    val customerName: String = ""
)

@ReadModel
@FromEvent(OrderCreatedForKey::class, key = "orderNumbr")
private data class TypoKeyReadModel(val orderNumber: String = "")

@EventType
private data class AccountOpenedForWildcard(val accountName: String)

@EventType
private class AccountPingedForWildcard

@ReadModel
@FromEvent(AccountOpenedForWildcard::class)
@FromEvent(AccountPingedForWildcard::class)
private data class WildcardAccount(
    @SetFrom("accountName")
    val accountName: String = ""
)

class ProjectionsServiceTests {

    private fun register(vararg classes: KClass<*>): List<ProjectionsOuterClass.ProjectionDefinition> {
        val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
        val request = slot<ProjectionsOuterClass.RegisterRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()
        val service = ProjectionsService("my-store", stub, mockk<io.cratis.chronicle.readModels.ReadModelsService>(relaxed = true), "default")
        runBlocking { service.register(*classes) }
        return request.captured.projectionsList
    }

    private fun registerOne(cls: KClass<*>): ProjectionsOuterClass.ProjectionDefinition = register(cls).single()

    private fun ProjectionsOuterClass.ProjectionDefinition.fromFor(eventType: KClass<*>): ProjectionsOuterClass.FromDefinition =
        fromList.first { it.key.id == eventType.simpleName }.value

    // --- @FromEvent key resolution ---

    @Test
    fun `FromEvent defaults the key to EventSourceId`() {
        assertEquals(EVENT_SOURCE_ID_KEY, registerOne(Ticket::class).fromFor(TicketOpened::class).key)
    }

    @Test
    fun `FromEvent honors an explicit key`() {
        assertEquals("orderNumber", registerOne(OrderByNumber::class).fromFor(OrderCreatedForKey::class).key)
    }

    // --- @Join ---

    @Test
    fun `Join merges every joined property for the same event type into one JoinDefinition`() {
        val definition = registerOne(InvoiceWithCustomer::class)
        val join = definition.joinList.first { it.key.id == CustomerRegistered::class.simpleName }.value
        assertEquals("customerId", join.on)
        assertEquals(EVENT_SOURCE_ID_KEY, join.key)
        assertEquals(mapOf("customerName" to "name"), join.propertiesMap)
    }

    // --- @ChildrenFrom ---

    @Test
    fun `ChildrenFrom builds a ChildrenDefinition keyed by the property name`() {
        val definition = registerOne(ShoppingCart::class)
        val children = definition.childrenMap.getValue("items")
        assertEquals("itemId", children.identifiedBy)
        val from = children.fromList.first { it.key.id == ItemAddedToCart::class.simpleName }.value
        assertEquals("itemId", from.key)
        assertEquals(mapOf("name" to "name"), from.propertiesMap)
    }

    // --- @Nested ---

    @Test
    fun `Nested builds a ChildrenDefinition from the nested type's own FromEvent`() {
        val definition = registerOne(Employee::class)
        val nested = definition.nestedMap.getValue("contract")
        val from = nested.fromList.first { it.key.id == ContractSigned::class.simpleName }.value
        assertEquals(mapOf("title" to "title"), from.propertiesMap)
    }

    @Test
    fun `ClearWith on the nested type registers a RemovedWith entry clearing on EventSourceId`() {
        val definition = registerOne(Employee::class)
        val nested = definition.nestedMap.getValue("contract")
        val removedWith = nested.removedWithList.single().value
        assertEquals(EVENT_SOURCE_ID_KEY, removedWith.key)
    }

    // --- Counters ---

    @Test
    fun `Count writes the count expression`() {
        assertEquals("\$count", registerOne(PageStats::class).fromFor(PageViewed::class).propertiesMap["views"])
    }

    @Test
    fun `Increment writes the increment expression`() {
        assertEquals("\$increment", registerOne(PageStats::class).fromFor(ItemLiked::class).propertiesMap["likes"])
    }

    @Test
    fun `Decrement writes the decrement expression`() {
        assertEquals("\$decrement", registerOne(PageStats::class).fromFor(ItemUnliked::class).propertiesMap["likes"])
    }

    @Test
    fun `AddFrom writes an add expression against the event property`() {
        assertEquals("\$add(amount)", registerOne(PageStats::class).fromFor(CreditsAdded::class).propertiesMap["credits"])
    }

    @Test
    fun `SubtractFrom writes a subtract expression against the event property`() {
        assertEquals("\$subtract(amount)", registerOne(PageStats::class).fromFor(CreditsSpent::class).propertiesMap["credits"])
    }

    @Test
    fun `a constantKey on Count overrides the key with a dollar-value expression`() {
        assertEquals("\$value(singleton)", registerOne(GlobalPageViews::class).fromFor(PageViewed::class).key)
    }

    // --- FromEvery / FromAll ---

    @Test
    fun `FromEvery maps a context property across every subscribed event`() {
        val all = registerOne(WidgetAudit::class).all
        assertEquals("\$eventContext(occurred)", all.propertiesMap["lastTouchedAt"])
    }

    @Test
    fun `FromEventSourceId maps the key across every subscribed event`() {
        val all = registerOne(WidgetAudit::class).all
        assertEquals("\$eventSourceId", all.propertiesMap["id"])
    }

    @Test
    fun `FromAll defaults to the property's own name`() {
        val all = registerOne(WidgetAudit::class).all
        assertEquals("name", all.propertiesMap["name"])
    }

    // --- @RemovedWith ---

    @Test
    fun `RemovedWith registers a removal keyed on EventSourceId by default`() {
        val removedWith = registerOne(Subscription::class).removedWithList.single()
        assertEquals(SubscriptionCancelled::class.simpleName, removedWith.key.id)
        assertEquals(EVENT_SOURCE_ID_KEY, removedWith.value.key)
    }

    // --- @NotRewindable ---

    @Test
    fun `NotRewindable turns off rewindability`() {
        assertFalse(registerOne(ForwardOnlyLedger::class).isRewindable)
    }

    @Test
    fun `a read model is rewindable by default`() {
        assertTrue(registerOne(RewindableLedger::class).isRewindable)
    }

    // --- @NoAutoMap ---

    @Test
    fun `NoAutoMap on the class disables AutoMap for the whole read model`() {
        assertEquals(ProjectionsOuterClass.AutoMap.Disabled, registerOne(ManualOnlyReadModel::class).autoMap)
    }

    @Test
    fun `a read model AutoMaps by default`() {
        assertEquals(ProjectionsOuterClass.AutoMap.Enabled, registerOne(RewindableLedger::class).autoMap)
    }

    @Test
    fun `NoAutoMap on a root property excludes just that property`() {
        assertTrue(registerOne(WidgetWithGuardedName::class).noAutoMapPropertiesList.contains("name"))
    }

    @Test
    fun `NoAutoMap on a property of a ChildrenFrom element type excludes that property on the child`() {
        val children = registerOne(TaskList::class).childrenMap.getValue("tasks")
        assertTrue(children.noAutoMapPropertiesList.contains("name"))
    }

    @Test
    fun `NoAutoMap on a property of a Nested type excludes that property on the nested object`() {
        val nested = registerOne(EmployeeWithGuardedContract::class).nestedMap.getValue("contract")
        assertTrue(nested.noAutoMapPropertiesList.contains("title"))
    }

    // --- @SetValue ---

    @Test
    fun `SetValue writes the constant as a dollar-value expression per event`() {
        val definition = registerOne(OrderStatus::class)
        assertEquals("\$value(active)", definition.fromFor(OrderPlacedForStatus::class).propertiesMap["status"])
        assertEquals("\$value(cancelled)", definition.fromFor(OrderCancelledForStatus::class).propertiesMap["status"])
    }

    @Test
    fun `SetValue with clear writes the null expression`() {
        val definition = registerOne(NotableThing::class)
        assertEquals("\$null", definition.fromFor(NoteCleared::class).propertiesMap["note"])
    }

    // --- @SetFromContext ---

    @Test
    fun `SetFromContext maps a named event context property for a specific event`() {
        assertEquals(
            "\$eventContext(occurred)",
            registerOne(AuditedOrder::class).fromFor(OrderPlacedForAudit::class).propertiesMap["orderedAt"]
        )
    }

    @Test
    fun `SetFromContext defaults the context property to the annotated property's own name`() {
        assertEquals(
            "\$eventContext(occurred)",
            registerOne(AuditedOrderDefaultContext::class).fromFor(OrderPlacedForAudit::class).propertiesMap["occurred"]
        )
    }

    @Test
    fun `SetFromContext with an unknown context property fails registration`() {
        assertThrows(InvalidPropertyForType::class.java) { registerOne(BadContextReadModel::class) }
    }

    // --- @Passive ---

    @Test
    fun `Passive marks the projection inactive`() {
        assertFalse(registerOne(PassiveSnapshot::class).isActive)
    }

    @Test
    fun `a read model is active by default`() {
        assertTrue(registerOne(RewindableLedger::class).isActive)
    }

    // --- Property validation ---

    @Test
    fun `a typo in an explicit SetFrom property path fails registration`() {
        assertThrows(InvalidPropertyForType::class.java) { registerOne(TypoReadModel::class) }
    }

    @Test
    fun `a typo in a Join event property name fails registration`() {
        assertThrows(InvalidPropertyForType::class.java) { registerOne(TypoJoinReadModel::class) }
    }

    @Test
    fun `a typo in an explicit FromEvent key fails registration`() {
        assertThrows(InvalidPropertyForType::class.java) { registerOne(TypoKeyReadModel::class) }
    }

    @Test
    fun `a wildcard SetFrom without an explicit event type is not validated against every subscribed event`() {
        // AccountPingedForWildcard carries no accountName property - the implicit form is documented to
        // apply only where a matching property exists, so this must not throw.
        assertEquals("accountName", registerOne(WildcardAccount::class).fromFor(AccountOpenedForWildcard::class).propertiesMap["accountName"])
    }
}
