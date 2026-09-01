// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendedEvent;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.java.ComplianceServiceJavaBridge;
import io.cratis.chronicle.EventStore;

// Not a single @Pii anywhere in this file, and every personal value below is still encrypted at
// rest. The annotation lives once on each concept — FullName, EmailAddress, PhoneNumber,
// StreetAddress, City, PostalCode — and comes along with the type wherever it is used. Add a third
// event carrying an EmailAddress tomorrow and it is encrypted without anyone remembering to
// annotate it, which is the failure mode property-level annotations have: the annotation is only
// ever as complete as the last person to add a property.
//
// The types also stop the compiler treating a name and an email as the same thing — CustomerDetails
// below takes eight values in a row, and swapping any two of them no longer compiles.

@EventType
record CustomerRegistered(
    CustomerId customerId,
    EmailAddress email,
    FullName fullName,
    PhoneNumber phoneNumber
) {}

@EventType
record CustomerAddressUpdated(
    CustomerId customerId,
    StreetAddress streetAddress,
    City city,
    PostalCode postalCode,
    Country country
) {}

@ReadModel
class Customer {
    private CustomerId id = new CustomerId("");
    private FullName fullName = new FullName("");
    private EmailAddress email = new EmailAddress("");
    private PhoneNumber phoneNumber = new PhoneNumber("");
    private StreetAddress streetAddress = new StreetAddress("");
    private City city = new City("");
    private PostalCode postalCode = new PostalCode("");
    private Country country = new Country("");
    private String customerNumber = "";
    private String accountStatus = "active";
    private int totalOrders = 0;

    public Customer() {}

    public CustomerId getId() { return id; }
    public void setId(CustomerId id) { this.id = id; }

    public FullName getFullName() { return fullName; }
    public void setFullName(FullName fullName) { this.fullName = fullName; }

    public EmailAddress getEmail() { return email; }
    public void setEmail(EmailAddress email) { this.email = email; }

    public PhoneNumber getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(PhoneNumber phoneNumber) { this.phoneNumber = phoneNumber; }

    public StreetAddress getStreetAddress() { return streetAddress; }
    public void setStreetAddress(StreetAddress streetAddress) { this.streetAddress = streetAddress; }

    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }

    public PostalCode getPostalCode() { return postalCode; }
    public void setPostalCode(PostalCode postalCode) { this.postalCode = postalCode; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
}

@ReadModel
class CustomerDetails {
    private CustomerId id = new CustomerId("");
    private FullName fullName = new FullName("");
    private EmailAddress email = new EmailAddress("");
    private PhoneNumber phoneNumber = new PhoneNumber("");
    private StreetAddress streetAddress = new StreetAddress("");
    private City city = new City("");
    private PostalCode postalCode = new PostalCode("");
    private Country country = new Country("");

    public CustomerDetails() {}

    public CustomerDetails(CustomerId id, FullName fullName, EmailAddress email, PhoneNumber phoneNumber,
                          StreetAddress streetAddress, City city, PostalCode postalCode, Country country) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.streetAddress = streetAddress;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public CustomerId getId() { return id; }
    public void setId(CustomerId id) { this.id = id; }

    public FullName getFullName() { return fullName; }
    public void setFullName(FullName fullName) { this.fullName = fullName; }

    public EmailAddress getEmail() { return email; }
    public void setEmail(EmailAddress email) { this.email = email; }

    public PhoneNumber getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(PhoneNumber phoneNumber) { this.phoneNumber = phoneNumber; }

    public StreetAddress getStreetAddress() { return streetAddress; }
    public void setStreetAddress(StreetAddress streetAddress) { this.streetAddress = streetAddress; }

    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }

    public PostalCode getPostalCode() { return postalCode; }
    public void setPostalCode(PostalCode postalCode) { this.postalCode = postalCode; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }
}

class SampleCustomerData {
    static final SampleCustomerData instance = new SampleCustomerData(
        new CustomerId("c0000001-0000-0000-0000-000000000000"),
        new FullName("Eve Jackson"),
        new EmailAddress("eve.jackson@example.com"),
        new PhoneNumber("+1-202-555-0143"),
        new StreetAddress("742 Evergreen Terrace"),
        new City("Springfield"),
        new PostalCode("49007"),
        new Country("USA")
    );

    final CustomerId id;
    final FullName fullName;
    final EmailAddress email;
    final PhoneNumber phoneNumber;
    final StreetAddress streetAddress;
    final City city;
    final PostalCode postalCode;
    final Country country;

    SampleCustomerData(CustomerId id, FullName fullName, EmailAddress email, PhoneNumber phoneNumber,
                       StreetAddress streetAddress, City city, PostalCode postalCode, Country country) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.streetAddress = streetAddress;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }
}

public class Compliance {
    public static void registerCustomerWithPii(IEventStore store) throws Exception {
        SampleCustomerData sampleCustomer = SampleCustomerData.instance;

        CustomerRegistered registered = new CustomerRegistered(
            sampleCustomer.id,
            sampleCustomer.email,
            sampleCustomer.fullName,
            sampleCustomer.phoneNumber
        );

        CustomerAddressUpdated addressUpdated = new CustomerAddressUpdated(
            sampleCustomer.id,
            sampleCustomer.streetAddress,
            sampleCustomer.city,
            sampleCustomer.postalCode,
            sampleCustomer.country
        );

        // The event source id is a String on the wire and always will be, so the blocking Java
        // bridge takes one — getValue() is where a concept goes back to being a plain value.
        String eventSourceId = sampleCustomer.id.getValue();
        AppendResult result1 = EventLogJavaBridge.append(store.getEventLog(), eventSourceId, registered, null);
        AppendResult result2 = EventLogJavaBridge.append(store.getEventLog(), eventSourceId, addressUpdated, null);

        if (!result1.isSuccess() || !result2.isSuccess()) {
            StringBuilder violations = new StringBuilder();
            if (!result1.isSuccess()) {
                violations.append(result1.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining("; ")));
            }
            if (!result2.isSuccess()) {
                if (violations.length() > 0) violations.append("; ");
                violations.append(result2.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining("; ")));
            }
            System.out.println("[pii] Could not register " + sampleCustomer.fullName.getValue() + ": " + violations);
            return;
        }

        long lastSeq = EventLogJavaBridge.getSequenceNumber(result2);
        System.out.println("[pii] Registered " + sampleCustomer.fullName.getValue() + " (" +
                          eventSourceId + ") with PII events up to sequence " + lastSeq);
    }

    public static void showCustomerReadModel(IEventStore store) throws Exception {
        SampleCustomerData sampleCustomer = SampleCustomerData.instance;
        CustomerDetails customer = ReadModelsJavaBridge.getInstanceByKey(
            store.getReadModels(),
            CustomerDetails.class,
            sampleCustomer.id.getValue()
        );

        if (customer == null || customer.getId().getValue().isEmpty()) {
            System.out.println("[pii] No CustomerDetails read model found for " +
                             sampleCustomer.id.getValue() + ". Register the customer first (press C).");
            return;
        }

        System.out.println("Customer read model for " + customer.getId().getValue() + ":");
        System.out.println(fmt("Full name", customer.getFullName()));
        System.out.println(fmt("Email", customer.getEmail()));
        System.out.println(fmt("Phone number", customer.getPhoneNumber()));
        System.out.println(fmt("Street address", customer.getStreetAddress()));
        System.out.println(fmt("City", customer.getCity()));
        System.out.println(fmt("Postal code", customer.getPostalCode()));
        System.out.println(fmt("Country", customer.getCountry()));
        System.out.println("  Every [PII] value above is encrypted at rest and decrypted on read. Press K to delete the");
        System.out.println("  encryption key and view this again — the PII comes back empty, Country is untouched.");
    }

    /**
     * Deletes the encryption key used for the sample customer's PII — a real "right to be forgotten" erasure.
     * Existing encrypted PII values become permanently unreadable; no re-encryption or rollback is possible.
     */
    public static void deleteCustomerEncryptionKey(EventStore store) {
        SampleCustomerData sampleCustomer = SampleCustomerData.instance;
        ComplianceServiceJavaBridge.deleteEncryptionKey(store.getCompliance(), sampleCustomer.id.getValue());
        System.out.println("[pii] Deleted the encryption key for " + sampleCustomer.fullName.getValue() + " (" +
                          sampleCustomer.id.getValue() + "). Its encrypted PII can no longer be decrypted.");
    }

    /**
     * Permanently redacts the most recent address-change event for {@code person}.
     * <p>
     * Redaction is a destructive content rewrite, not a soft delete or a field mask — once this
     * returns, the original address content for that specific event is gone from the event store
     * for good. Demonstrates {@code getForEventSourceIdAndEventTypes} to locate the event's
     * sequence number, then {@code redact} to erase its content.
     */
    public static void redactLastAddressChange(IEventStore store, Person person) throws Exception {
        List<AppendedEvent> addressEvents = EventLogJavaBridge.getForEventSourceIdAndEventTypes(
            store.getEventLog(),
            person.getId(),
            List.of(EmployeeAddressSet.class, EmployeeMoved.class)
        );
        AppendedEvent last = addressEvents.stream()
            .max(Comparator.comparingLong(e -> e.getContext().getSequenceNumber()))
            .orElse(null);
        if (last == null) {
            System.out.println("[redact] " + person.getFirstName() + " " + person.getLastName() +
                              " has no address-change events to redact.");
            return;
        }
        long sequenceNumber = last.getContext().getSequenceNumber();
        EventLogJavaBridge.redact(store.getEventLog(), sequenceNumber, "Sample: erase address history");
        System.out.println("[redact] Permanently redacted the address event at sequence " + sequenceNumber +
                          " for " + person.getFirstName() + " " + person.getLastName() +
                          ". The original content is gone — this cannot be undone.");
    }

    /**
     * Permanently redacts every event for the sample customer — a full "right to be forgotten" erasure.
     * <p>
     * More thorough than {@link #deleteCustomerEncryptionKey}: instead of leaving encrypted-but-unreadable
     * content behind, this rewrites every event's content for the event source, gone for good. Like
     * {@link #redactLastAddressChange}, this is destructive and irreversible — only use it for a
     * confirmed compliance/erasure request.
     */
    public static void redactAllCustomerEvents(IEventStore store) throws Exception {
        SampleCustomerData sampleCustomer = SampleCustomerData.instance;
        EventLogJavaBridge.redactForEventSource(store.getEventLog(), sampleCustomer.id.getValue(),
            "Sample: GDPR erasure request", List.of());
        System.out.println("[redact] Permanently redacted every event for " + sampleCustomer.fullName.getValue() + " (" +
                          sampleCustomer.id.getValue() + "). This cannot be undone.");
    }

    /**
     * Formats one value, reading the {@code [PII]} marker off the concept's own type.
     * <p>
     * Nothing here keeps a list of which fields are personal — {@link Pii} is metadata on the type,
     * so anything that needs to know can ask, and it can never fall out of date with what is
     * actually encrypted. Chronicle's schema generator answers the same question the same way.
     */
    private static String fmt(String label, ConceptAs<String> value) {
        Pii pii = value.getClass().getAnnotation(Pii.class);
        String marker = pii != null ? "   [PII: " + pii.description() + "]" : "";
        String displayValue = value.getValue().isEmpty() ? "(empty)" : value.getValue();
        return "  " + String.format("%-15s", label) + ": " + displayValue + marker;
    }
}
