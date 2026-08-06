// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendedEvent;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.java.ComplianceServiceJavaBridge;
import io.cratis.chronicle.EventStore;

@EventType
record CustomerRegistered(
    String customerId,
    @Pii(description = "Customer email address") String email,
    @Pii(description = "Customer full legal name") String fullName,
    @Pii(description = "Customer phone contact number") String phoneNumber
) {}

@EventType
record CustomerAddressUpdated(
    String customerId,
    @Pii(description = "Customer street address") String streetAddress,
    @Pii(description = "City of residence") String city,
    @Pii(description = "Postal code") String postalCode,
    String country
) {}

@ReadModel
class Customer {
    private String id = "";
    @Pii(description = "Customer full legal name")
    private String fullName = "";
    @Pii(description = "Customer email address")
    private String email = "";
    @Pii(description = "Customer phone contact number")
    private String phoneNumber = "";
    @Pii(description = "Customer street address")
    private String streetAddress = "";
    @Pii(description = "City of residence")
    private String city = "";
    @Pii(description = "Postal code")
    private String postalCode = "";
    private String country = "";
    private String customerNumber = "";
    private String accountStatus = "active";
    private int totalOrders = 0;

    public Customer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
}

@ReadModel
class CustomerDetails {
    private String id = "";
    @Pii(description = "Customer full legal name")
    private String fullName = "";
    @Pii(description = "Customer email address")
    private String email = "";
    @Pii(description = "Customer phone contact number")
    private String phoneNumber = "";
    @Pii(description = "Customer street address")
    private String streetAddress = "";
    @Pii(description = "City of residence")
    private String city = "";
    @Pii(description = "Postal code")
    private String postalCode = "";
    private String country = "";

    public CustomerDetails() {}

    public CustomerDetails(String id, String fullName, String email, String phoneNumber,
                          String streetAddress, String city, String postalCode, String country) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.streetAddress = streetAddress;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}

class SampleCustomerData {
    static final SampleCustomerData instance = new SampleCustomerData(
        "c0000001-0000-0000-0000-000000000000",
        "Eve Jackson",
        "eve.jackson@example.com",
        "+1-202-555-0143",
        "742 Evergreen Terrace",
        "Springfield",
        "49007",
        "USA"
    );

    final String id;
    final String fullName;
    final String email;
    final String phoneNumber;
    final String streetAddress;
    final String city;
    final String postalCode;
    final String country;

    SampleCustomerData(String id, String fullName, String email, String phoneNumber,
                       String streetAddress, String city, String postalCode, String country) {
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
        
        AppendResult result1 = EventLogJavaBridge.append(store.getEventLog(), sampleCustomer.id, registered, null);
        AppendResult result2 = EventLogJavaBridge.append(store.getEventLog(), sampleCustomer.id, addressUpdated, null);
        
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
            System.out.println("[pii] Could not register " + sampleCustomer.fullName + ": " + violations);
            return;
        }
        
        long lastSeq = EventLogJavaBridge.getSequenceNumber(result2);
        System.out.println("[pii] Registered " + sampleCustomer.fullName + " (" + 
                          sampleCustomer.id + ") with PII events up to sequence " + lastSeq);
    }

    public static void showCustomerReadModel(IEventStore store) throws Exception {
        SampleCustomerData sampleCustomer = SampleCustomerData.instance;
        CustomerDetails customer = ReadModelsJavaBridge.getInstanceByKey(
            store.getReadModels(),
            CustomerDetails.class, 
            sampleCustomer.id
        );
        
        if (customer == null || customer.getId().isEmpty()) {
            System.out.println("[pii] No CustomerDetails read model found for " + 
                             sampleCustomer.id + ". Register the customer first (press C).");
            return;
        }

        System.out.println("Customer read model for " + customer.getId() + ":");
        System.out.println(fmt("Full name", customer.getFullName(), true));
        System.out.println(fmt("Email", customer.getEmail(), true));
        System.out.println(fmt("Phone number", customer.getPhoneNumber(), true));
        System.out.println(fmt("Street address", customer.getStreetAddress(), true));
        System.out.println(fmt("City", customer.getCity(), true));
        System.out.println(fmt("Postal code", customer.getPostalCode(), true));
        System.out.println(fmt("Country", customer.getCountry(), false));
        System.out.println("  PII fields are stored encrypted at rest — values above are the encrypted form.");
    }

    /**
     * Deletes the encryption key used for the sample customer's PII — a real "right to be forgotten" erasure.
     * Existing encrypted PII values become permanently unreadable; no re-encryption or rollback is possible.
     */
    public static void deleteCustomerEncryptionKey(EventStore store) {
        SampleCustomerData sampleCustomer = SampleCustomerData.instance;
        ComplianceServiceJavaBridge.deleteEncryptionKey(store.getCompliance(), sampleCustomer.id);
        System.out.println("[pii] Deleted the encryption key for " + sampleCustomer.fullName + " (" +
                          sampleCustomer.id + "). Its encrypted PII can no longer be decrypted.");
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
        EventLogJavaBridge.redactForEventSource(store.getEventLog(), sampleCustomer.id, "Sample: GDPR erasure request", List.of());
        System.out.println("[redact] Permanently redacted every event for " + sampleCustomer.fullName + " (" +
                          sampleCustomer.id + "). This cannot be undone.");
    }

    private static String fmt(String label, String value, boolean isPii) {
        String displayValue = value.isEmpty() ? "(empty)" : value;
        String piiMarker = isPii ? "   [PII]" : "";
        return "  " + String.format("%-15s", label) + ": " + displayValue + piiMarker;
    }
}
