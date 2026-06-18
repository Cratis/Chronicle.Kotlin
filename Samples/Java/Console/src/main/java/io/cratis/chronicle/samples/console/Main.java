// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.ChronicleClient;
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.EventStoreNamespaceName;
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.identity.Identity;
import io.cratis.chronicle.transactions.UnitOfWork;
import io.cratis.chronicle.auditing.CausationType;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cratis.chronicle.auditing.CausationManagerKt.getCausationManager;
import static io.cratis.chronicle.identity.IdentityProviderKt.getIdentityProvider;
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.java.TransactionalEventSequenceJavaBridge;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.java.ConstraintBuilderJavaBridge;
import io.cratis.chronicle.java.EventTypesServiceJavaBridge;
import io.cratis.chronicle.java.ConstraintsServiceJavaBridge;
import io.cratis.chronicle.java.UnitOfWorkJavaBridge;
import io.cratis.chronicle.java.ReactorsServiceJavaBridge;
import io.cratis.chronicle.java.ReducersServiceJavaBridge;
import io.cratis.chronicle.java.ProjectionsServiceJavaBridge;
import io.cratis.chronicle.java.NamespacesServiceJavaBridge;
import io.cratis.chronicle.java.EventSeedingServiceJavaBridge;
import io.cratis.chronicle.java.CausationManagerJavaBridge;

public class Main {
    private static final List<String> titles = Arrays.asList(
        "Software Engineer",
        "Senior Engineer",
        "Principal Engineer",
        "Engineering Manager",
        "Architect"
    );

    private static class Address {
        final String address;
        final String city;
        final String zipCode;
        final String country;

        Address(String address, String city, String zipCode, String country) {
            this.address = address;
            this.city = city;
            this.zipCode = zipCode;
            this.country = country;
        }
    }

    private static final List<Address> addresses = Arrays.asList(
        new Address("221B Baker Street", "London", "NW1 6XE", "UK"),
        new Address("1600 Amphitheatre Parkway", "Mountain View", "94043", "USA"),
        new Address("1 Infinite Loop", "Cupertino", "95014", "USA"),
        new Address("5 Wall Street", "New York", "10005", "USA")
    );

    private static final List<Identity> users = Arrays.asList(
        new Identity("u0000001-0000-0000-0000-000000000000", "Alice Smith", "alice.smith", null),
        new Identity("u0000002-0000-0000-0000-000000000000", "Bob Jones", "bob.jones", null),
        Identity.Companion.getSystem()
    );

    private static class SimpleRandom {
        private int seed;

        SimpleRandom() {
            this.seed = (int)(System.currentTimeMillis() & 0x7fffffffL);
        }

        int next(int max) {
            seed = (seed * 1664525 + 1013904223) & Integer.MAX_VALUE;
            return seed % max;
        }
    }

    private static void setupCausation(Identity user, String commandName, Map<String, String> properties) {
        getIdentityProvider().setCurrentIdentity(user);
        getCausationManager().defineRoot(Map.of("source", "console-sample"));
        CausationManagerJavaBridge.add(getCausationManager(), commandName, properties);
    }

    private static final List<String> seedTitles = Arrays.asList(
        "Software Engineer", "Senior Engineer", "Principal Engineer"
    );

    private static final List<Address> seedAddresses = Arrays.asList(
        new Address("221B Baker Street", "London", "NW1 6XE", "UK"),
        new Address("1600 Amphitheatre Parkway", "Mountain View", "94043", "USA"),
        new Address("1 Infinite Loop", "Cupertino", "95014", "USA")
    );

    /**
     * Ensures every seeded employee has events in the event log.
     * If the seeder's server-side deduplication state is stale (events were cleared but
     * grain state was not), falls back to appending the initial events directly.
     */
    private static void ensureSeededEmployees(IEventStore store) throws Exception {
        List<Person> employees = Employees.employees;
        for (int index = 0; index < employees.size(); index++) {
            Person employee = employees.get(index);
            boolean hasEvents = EventLogJavaBridge.hasEventsFor(store.getEventLog(), employee.getId());
            if (!hasEvents) {
                String title = seedTitles.get(index % seedTitles.size());
                Address addr = seedAddresses.get(index % seedAddresses.size());
                
                getIdentityProvider().setCurrentIdentity(Identity.Companion.getSystem());
                getCausationManager().defineRoot(Map.of("source", "console-sample-seed"));
                
                EventLogJavaBridge.append(store.getEventLog(), employee.getId(), 
                    new EmployeeHired(employee.getFirstName(), employee.getLastName(), title), null);
                EventLogJavaBridge.append(store.getEventLog(), employee.getId(), 
                    new EmployeeEmailSet(Employees.emailFor(employee)), null);
                EventLogJavaBridge.append(store.getEventLog(), employee.getId(), 
                    new EmployeeAddressSet(addr.address, addr.city, addr.zipCode, addr.country), null);
            }
        }
    }

    private static void promote(IEventStore store, Person person, Identity user, SimpleRandom random) throws Exception {
        String title = titles.get(random.next(titles.size()));
        setupCausation(user, "ConsoleSample.Commands.Promote", Map.of("employeeId", person.getId()));
        AppendResult result = EventLogJavaBridge.append(store.getEventLog(), person.getId(), new EmployeePromoted(title), null);
        System.out.println("[" + person.getId() + "] Promoted " + person.getFirstName() + " " + 
                          person.getLastName() + " to '" + title + "' at sequence " + 
                          EventLogJavaBridge.getSequenceNumber(result) + "  [caused-by: " + user.getUserName() + "]");
    }

    private static void move(IEventStore store, Person person, Identity user, SimpleRandom random) throws Exception {
        Address addr = addresses.get(random.next(addresses.size()));
        setupCausation(user, "ConsoleSample.Commands.Move", Map.of("employeeId", person.getId()));
        AppendResult result = EventLogJavaBridge.append(store.getEventLog(), person.getId(), 
            new EmployeeMoved(addr.address, addr.city, addr.zipCode, addr.country), null);
        System.out.println("[" + person.getId() + "] Moved " + person.getFirstName() + " " + 
                          person.getLastName() + " to " + addr.address + ", " + addr.city + 
                          ""  + 
                          "  [caused-by: " + user.getUserName() + "]");
    }

    private static void setEmail(IEventStore store, Person person, Identity user) throws Exception {
        String email = Employees.emailFor(person);
        setupCausation(user, "ConsoleSample.Commands.SetEmail", Map.of("employeeId", person.getId()));
        AppendResult result = EventLogJavaBridge.append(store.getEventLog(), person.getId(), new EmployeeEmailSet(email), null);
        if (result.isSuccess()) {
            System.out.println("[" + person.getId() + "] Set " + person.getFirstName() + " " + 
                              person.getLastName() + "'s email to " + email + " at sequence " + 
                              EventLogJavaBridge.getSequenceNumber(result) + "  [caused-by: " + user.getUserName() + "]");
        } else {
            String violations = String.join("; ", 
                result.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .toList());
            System.out.println("[" + person.getId() + "] Could not set email: " + violations);
        }
    }

    private static void stealEmail(IEventStore store, int selectedIndex, Identity user) throws Exception {
        List<Person> employees = Employees.employees;
        Person person = employees.get(selectedIndex);
        Person victim = employees.get((selectedIndex + 1) % employees.size());
        String email = Employees.emailFor(victim);
        
        setupCausation(user, "ConsoleSample.Commands.SetEmail", Map.of("employeeId", person.getId()));
        AppendResult result = EventLogJavaBridge.append(store.getEventLog(), person.getId(), new EmployeeEmailSet(email), null);
        
        if (result.isSuccess()) {
            System.out.println("[" + person.getId() + "] Unexpectedly took " + email + 
                              ""  + 
                              "  [caused-by: " + user.getUserName() + "]");
        } else {
            String violations = String.join("; ", 
                result.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .toList());
            System.out.println("[" + person.getId() + "] Rejected taking " + victim.getFirstName() + 
                              "'s email (" + email + "): " + violations);
        }
    }

    private static void transact(IEventStore store, int selectedIndex, Identity user, SimpleRandom random) throws Exception {
        List<Person> employees = Employees.employees;
        Person selected = employees.get(selectedIndex);
        Person alsoUpdate = employees.get((selectedIndex + 1) % employees.size());
        
        String selectedTitle = titles.get(random.next(titles.size()));
        Address selectedAddr = addresses.get(random.next(addresses.size()));
        String secondTitle = titles.get(random.next(titles.size()));

        setupCausation(user, "ConsoleSample.Commands.BulkUpdate", 
            Map.of("employees", selected.getId() + "," + alsoUpdate.getId()));

        UnitOfWork unitOfWork = store.getUnitOfWorkManager().begin();
        TransactionalEventSequenceJavaBridge.append(store.getEventLog().getTransactional(), selected.getId(), new EmployeePromoted(selectedTitle), null);
        TransactionalEventSequenceJavaBridge.appendMany(store.getEventLog().getTransactional(), selected.getId(), 
            Arrays.asList(new EmployeeMoved(selectedAddr.address, selectedAddr.city, 
                                           selectedAddr.zipCode, selectedAddr.country)), null);
        TransactionalEventSequenceJavaBridge.append(store.getEventLog().getTransactional(), alsoUpdate.getId(), new EmployeePromoted(secondTitle), null);
        UnitOfWorkJavaBridge.commit(unitOfWork);

        System.out.println("[transaction] Committed staged events for " + selected.getFirstName() + " " + 
                          selected.getLastName() + " and " + alsoUpdate.getFirstName() + " " + 
                          alsoUpdate.getLastName() + "  [caused-by: " + user.getUserName() + "]");
    }

    private static void readModel(IEventStore store, Person person) throws Exception {
        EmployeeState state = ReadModelsJavaBridge.getInstanceByKey(store.getReadModels(), EmployeeState.class, person.getId());
        if (state == null) {
            System.out.println("[read-model] No state found for " + person.getFirstName() + " " + 
                              person.getLastName() + " yet.");
        } else {
            String emailDisplay = state.getEmail().isEmpty() ? "no email yet" : state.getEmail();
            String addressDisplay = state.getAddress().isEmpty() ? "no address yet" : state.getAddress();
            System.out.println("[read-model] " + person.getFirstName() + " " + person.getLastName() + 
                              ": " + state.getTitle() + " <" + emailDisplay + "> @ " + addressDisplay);
        }
    }

    private static void writeInstructions() {
        System.out.println("""

Use 1-3 to select an employee. Then:
  P = Promote          A = Move (change address)
  E = Set email        U = Try to take the next employee's email (constraint violation)
  R = Read model       T = Transactional update
  C = Register customer with PII   V = View customer PII read model
  I = Switch user (cycle: Alice Smith -> Bob Jones -> System)
  H or ? = Show this menu          Q = Quit
""");
    }

    private static void writeSelectedEmployee(int selectedIndex, int userIndex) {
        List<Person> employees = Employees.employees;
        Person person = employees.get(selectedIndex);
        Identity user = users.get(userIndex);
        System.out.println("Selected  [" + (selectedIndex + 1) + "] " + person.getFirstName() + " " + 
                          person.getLastName() + " (" + person.getId() + ")");
        System.out.println("Acting as [" + (userIndex + 1) + "] " + user.getName() + " (@" + 
                          user.getUserName() + ")");
    }

    private static void writeSelectedUser(int userIndex) {
        Identity user = users.get(userIndex);
        System.out.println("\nSwitched to user [" + (userIndex + 1) + "] " + user.getName() + " (@" + 
                          user.getUserName() + ")");
    }

    private static String readKey() throws IOException {
        int ch = System.in.read();
        return ch == -1 ? "q" : String.valueOf((char)ch);
    }

    public static void main(String[] args) throws Exception {
        String connectionString = System.getenv("CHRONICLE_CONNECTION");
        ChronicleOptions options;
        if (connectionString != null) {
            options = ChronicleOptions.Companion.fromConnectionString(connectionString);
        } else {
            options = ChronicleOptions.Companion.development();
        }

        System.out.println("Connecting to Chronicle at " + options.getConnectionString().getTarget() + 
                          " (disableTls=" + options.getConnectionString().getDisableTls() + ")");
        
        ChronicleClient client = new ChronicleClient(options);

        try {
            EventStore store = (EventStore)client.getEventStore("TestStoreJava", "Default");

            System.out.println("Event store ready: " + store.getName() + " / " + store.getNamespace());

            // Register event type schemas first — Chronicle requires them before events can be appended.
            EventTypesServiceJavaBridge.register(store.getEventTypes(),
                EmployeeHired.class,
                EmployeeAddressSet.class,
                EmployeePromoted.class,
                EmployeeEmailSet.class,
                EmployeeMoved.class,
                CustomerRegistered.class,
                CustomerAddressUpdated.class
            );

            // Customer has no reducer or projection — register it explicitly so Chronicle knows its schema.
            ReadModelsJavaBridge.register(store.getReadModels(), Customer.class);
            ReactorsServiceJavaBridge.register(store.getReactors(), new HrNotificationReactor());
            // Reducers auto-register their read models (EmployeeState, CustomerDetails) with observerType=Reducer.
            ReducersServiceJavaBridge.register(store.getReducers(), new EmployeeStateReducer());
            ReducersServiceJavaBridge.register(store.getReducers(), new CustomerReducer());
            // Declarative projection: a separate class implements IProjectionFor<Employee>.
            ProjectionsServiceJavaBridge.register(store.getProjections(), new EmployeeListProjection());
            // Model-bound projection: EmployeeDetails carries @FromEvent/@SetFrom — no separate projection class needed.
            ProjectionsServiceJavaBridge.register(store.getProjections(), EmployeeDetails.class);
            // Ensure the Default namespace exists so the seeding grain can distribute seeds to it.
            NamespacesServiceJavaBridge.ensure(store.getNamespaces(), "Default");
            EventSeedingServiceJavaBridge.seed(store.getSeeding(), new EmployeeSeeder());
            Thread.sleep(2000);
            ensureSeededEmployees(store);
            // Register constraints AFTER seeding so the reindex job can find existing email events
            // and populate the global uniqueness index before the user can interact.
            ConstraintsServiceJavaBridge.register(store.getConstraints(), new UniqueEmployeeHire(), new UniqueEmployeeEmail());
            // Allow time for the reindex job to complete before allowing user interaction.
            Thread.sleep(3000);

            SimpleRandom random = new SimpleRandom();
            int selectedIndex = 0;
            int userIndex = 0;

            writeInstructions();
            writeSelectedEmployee(selectedIndex, userIndex);

            while (true) {
                String key = readKey().toLowerCase();

                switch (key) {
                    case "", "q" -> { 
                        System.out.println("Exiting..."); 
                        return; 
                    }
                    case "1" -> { 
                        selectedIndex = 0; 
                        writeSelectedEmployee(selectedIndex, userIndex); 
                    }
                    case "2" -> { 
                        selectedIndex = 1; 
                        writeSelectedEmployee(selectedIndex, userIndex); 
                    }
                    case "3" -> { 
                        selectedIndex = 2; 
                        writeSelectedEmployee(selectedIndex, userIndex); 
                    }
                    case "i" -> { 
                        userIndex = (userIndex + 1) % users.size(); 
                        writeSelectedUser(userIndex); 
                    }
                    case "p" -> promote(store, Employees.employees.get(selectedIndex), users.get(userIndex), random);
                    case "a" -> move(store, Employees.employees.get(selectedIndex), users.get(userIndex), random);
                    case "e" -> setEmail(store, Employees.employees.get(selectedIndex), users.get(userIndex));
                    case "u" -> stealEmail(store, selectedIndex, users.get(userIndex));
                    case "r" -> readModel(store, Employees.employees.get(selectedIndex));
                    case "t" -> transact(store, selectedIndex, users.get(userIndex), random);
                    case "c" -> Compliance.registerCustomerWithPii(store);
                    case "v" -> Compliance.showCustomerReadModel(store);
                    case "h", "?" -> writeInstructions();
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.dispose();
            System.out.println("Disconnected");
        }
    }
}
