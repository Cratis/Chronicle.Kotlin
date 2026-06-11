// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.EventStore
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.identity.identityProvider
import kotlinx.coroutines.runBlocking

private val titles = listOf(
    "Software Engineer",
    "Senior Engineer",
    "Principal Engineer",
    "Engineering Manager",
    "Architect"
)

private val addresses = listOf(
    Triple("221B Baker Street",         "London",        "NW1 6XE"  ) to "UK",
    Triple("1600 Amphitheatre Parkway", "Mountain View", "94043"    ) to "USA",
    Triple("1 Infinite Loop",           "Cupertino",     "95014"    ) to "USA",
    Triple("5 Wall Street",             "New York",      "10005"    ) to "USA"
)

private val users = listOf(
    Identity("u0000001-0000-0000-0000-000000000000", "Alice Smith", "alice.smith"),
    Identity("u0000002-0000-0000-0000-000000000000", "Bob Jones",   "bob.jones"),
    Identity.system
)

class Random {
    private var seed: Int = (System.currentTimeMillis() and 0x7fffffffL).toInt()
    fun next(max: Int): Int {
        seed = (seed * 1664525 + 1013904223) and Int.MAX_VALUE
        return seed % max
    }
}

private fun setupCausation(user: Identity, commandName: String, properties: Map<String, String>) {
    identityProvider.setCurrentIdentity(user)
    causationManager.defineRoot(mapOf("source" to "console-sample"))
    causationManager.add(CausationType(commandName), properties)
}

private val seedTitles = listOf("Software Engineer", "Senior Engineer", "Principal Engineer")
private val seedAddresses = listOf(
    Triple("221B Baker Street",         "London",        "NW1 6XE"  ) to "UK",
    Triple("1600 Amphitheatre Parkway", "Mountain View", "94043"    ) to "USA",
    Triple("1 Infinite Loop",           "Cupertino",     "95014"    ) to "USA"
)

/**
 * Ensures every seeded employee has events in the event log.
 * If the seeder's server-side deduplication state is stale (events were cleared but
 * grain state was not), falls back to appending the initial events directly.
 */
private suspend fun ensureSeededEmployees(store: IEventStore) {
    employees.forEachIndexed { index, employee ->
        val hasEvents = store.eventLog.hasEventsFor(employee.id)
        if (!hasEvents) {
            val title = seedTitles[index % seedTitles.size]
            val (addr, country) = seedAddresses[index % seedAddresses.size]
            val (address, city, zipCode) = addr
            identityProvider.setCurrentIdentity(Identity.system)
            causationManager.defineRoot(mapOf("source" to "console-sample-seed"))
            store.eventLog.append(employee.id, EmployeeHired(employee.firstName, employee.lastName, title))
            store.eventLog.append(employee.id, EmployeeEmailSet(emailFor(employee)))
            store.eventLog.append(employee.id, EmployeeAddressSet(address, city, zipCode, country))
        }
    }
}

private suspend fun promote(store: IEventStore, person: Person, user: Identity, random: Random) {
    val title = titles[random.next(titles.size)]
    setupCausation(user, "ConsoleSample.Commands.Promote", mapOf("employeeId" to person.id))
    val result = store.eventLog.append(person.id, EmployeePromoted(title))
    println("[${person.id}] Promoted ${person.firstName} ${person.lastName} to '$title' at sequence ${result.sequenceNumber.value}  [caused-by: ${user.userName}]")
}

private suspend fun move(store: IEventStore, person: Person, user: Identity, random: Random) {
    val (addr, country) = addresses[random.next(addresses.size)]
    val (address, city, zipCode) = addr
    setupCausation(user, "ConsoleSample.Commands.Move", mapOf("employeeId" to person.id))
    val result = store.eventLog.append(person.id, EmployeeMoved(address, city, zipCode, country))
    println("[${person.id}] Moved ${person.firstName} ${person.lastName} to $address, $city at sequence ${result.sequenceNumber.value}  [caused-by: ${user.userName}]")
}

private suspend fun setEmail(store: IEventStore, person: Person, user: Identity) {
    val email = emailFor(person)
    setupCausation(user, "ConsoleSample.Commands.SetEmail", mapOf("employeeId" to person.id))
    val result = store.eventLog.append(person.id, EmployeeEmailSet(email))
    if (result.isSuccess) {
        println("[${person.id}] Set ${person.firstName} ${person.lastName}'s email to $email at sequence ${result.sequenceNumber.value}  [caused-by: ${user.userName}]")
    } else {
        println("[${person.id}] Could not set email: ${result.constraintViolations.joinToString("; ") { it.message }}")
    }
}

private suspend fun stealEmail(store: IEventStore, selectedIndex: Int, user: Identity) {
    val person = employees[selectedIndex]
    val victim = employees[(selectedIndex + 1) % employees.size]
    val email = emailFor(victim)
    setupCausation(user, "ConsoleSample.Commands.SetEmail", mapOf("employeeId" to person.id))
    val result = store.eventLog.append(person.id, EmployeeEmailSet(email))
    if (result.isSuccess) {
        println("[${person.id}] Unexpectedly took $email at sequence ${result.sequenceNumber.value}  [caused-by: ${user.userName}]")
    } else {
        println("[${person.id}] Rejected taking ${victim.firstName}'s email ($email): ${result.constraintViolations.joinToString("; ") { it.message }}")
    }
}

private suspend fun transact(store: IEventStore, selectedIndex: Int, user: Identity, random: Random) {
    val selected = employees[selectedIndex]
    val alsoUpdate = employees[(selectedIndex + 1) % employees.size]
    val selectedTitle = titles[random.next(titles.size)]
    val (selectedAddr, selectedCountry) = addresses[random.next(addresses.size)]
    val (selectedAddress, selectedCity, selectedZip) = selectedAddr
    val secondTitle = titles[random.next(titles.size)]

    setupCausation(user, "ConsoleSample.Commands.BulkUpdate", mapOf("employees" to "${selected.id},${alsoUpdate.id}"))

    val unitOfWork = store.unitOfWorkManager.begin()
    store.eventLog.transactional.append(selected.id, EmployeePromoted(selectedTitle))
    store.eventLog.transactional.appendMany(selected.id, listOf(
        EmployeeMoved(selectedAddress, selectedCity, selectedZip, selectedCountry)
    ))
    store.eventLog.transactional.append(alsoUpdate.id, EmployeePromoted(secondTitle))
    unitOfWork.commit()

    println("[transaction] Committed staged events for ${selected.firstName} ${selected.lastName} and ${alsoUpdate.firstName} ${alsoUpdate.lastName}  [caused-by: ${user.userName}]")
}

private suspend fun readModel(store: IEventStore, person: Person) {
    val state = store.readModels.getInstanceByKey(EmployeeState::class, person.id)
    if (state == null) {
        println("[read-model] No state found for ${person.firstName} ${person.lastName} yet.")
    } else {
        println("[read-model] ${person.firstName} ${person.lastName}: ${state.title} <${state.email.ifEmpty { "no email yet" }}> @ ${state.address.ifEmpty { "no address yet" }}")
    }
}

private fun writeInstructions() {
    println("""

Use 1-3 to select an employee. Then:
  P = Promote          A = Move (change address)
  E = Set email        U = Try to take the next employee's email (constraint violation)
  R = Read model       T = Transactional update
  C = Register customer with PII   V = View customer PII read model
  I = Switch user (cycle: Alice Smith -> Bob Jones -> System)
  H or ? = Show this menu          Q = Quit
""".trimIndent())
}

private fun writeSelectedEmployee(selectedIndex: Int, userIndex: Int) {
    val person = employees[selectedIndex]
    val user = users[userIndex]
    println("Selected  [${selectedIndex + 1}] ${person.firstName} ${person.lastName} (${person.id})")
    println("Acting as [${userIndex + 1}] ${user.name} (@${user.userName})")
}

private fun writeSelectedUser(userIndex: Int) {
    val user = users[userIndex]
    println("\nSwitched to user [${userIndex + 1}] ${user.name} (@${user.userName})")
}

private fun readKey(): String {
    val ch = System.`in`.read()
    return if (ch == -1) "q" else ch.toChar().toString()
}

fun main() = runBlocking {
    val connectionString = System.getenv("CHRONICLE_CONNECTION")
    val options = if (connectionString != null) {
        ChronicleOptions.fromConnectionString(connectionString)
    } else {
        ChronicleOptions.development()
    }

    println("Connecting to Chronicle at ${options.connectionString.target} (disableTls=${options.connectionString.disableTls})")
    val client = ChronicleClient(options)

    try {
        val store = client.getEventStore("TestStoreKotlin") as EventStore

        println("Event store ready: ${store.name} / ${store.namespace}")

        // Register event type schemas first — Chronicle requires them before events can be appended.
        store.eventTypes.register(
            EmployeeHired::class,
            EmployeeAddressSet::class,
            EmployeePromoted::class,
            EmployeeEmailSet::class,
            EmployeeMoved::class,
            CustomerRegistered::class,
            CustomerAddressUpdated::class
        )

        store.readModels.register(EmployeeState::class, Employee::class, Customer::class, CustomerDetails::class)
        store.reactors.register(HrNotificationReactor())
        store.reducers.register(EmployeeStateReducer())
        store.reducers.register(CustomerReducer())
        store.projections.register(EmployeeListProjection())
        // Ensure the Default namespace exists so the seeding grain can distribute seeds to it.
        store.namespaces.ensure("Default")
        store.seeding.seed(EmployeeSeeder())
        kotlinx.coroutines.delay(2000)
        ensureSeededEmployees(store)
        // Register constraints AFTER seeding so the reindex job can find existing email events
        // and populate the global uniqueness index before the user can interact.
        store.constraints.register(UniqueEmployeeHire(), UniqueEmployeeEmail())
        // Allow time for the reindex job to complete before allowing user interaction.
        kotlinx.coroutines.delay(3000)

        val random = Random()
        var selectedIndex = 0
        var userIndex = 0

        writeInstructions()
        writeSelectedEmployee(selectedIndex, userIndex)

        while (true) {
            val key = readKey().lowercase()

            when (key) {
                "", "q" -> { println("Exiting..."); break }
                "1" -> { selectedIndex = 0; writeSelectedEmployee(selectedIndex, userIndex) }
                "2" -> { selectedIndex = 1; writeSelectedEmployee(selectedIndex, userIndex) }
                "3" -> { selectedIndex = 2; writeSelectedEmployee(selectedIndex, userIndex) }
                "i" -> { userIndex = (userIndex + 1) % users.size; writeSelectedUser(userIndex) }
                "p" -> promote(store, employees[selectedIndex], users[userIndex], random)
                "a" -> move(store, employees[selectedIndex], users[userIndex], random)
                "e" -> setEmail(store, employees[selectedIndex], users[userIndex])
                "u" -> stealEmail(store, selectedIndex, users[userIndex])
                "r" -> readModel(store, employees[selectedIndex])
                "t" -> transact(store, selectedIndex, users[userIndex], random)
                "c" -> registerCustomerWithPii(store)
                "v" -> showCustomerReadModel(store)
                "h", "?" -> writeInstructions()
            }
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.dispose()
        println("Disconnected")
    }
}
