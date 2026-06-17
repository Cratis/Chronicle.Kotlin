// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.AppendResult;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@EventType
class CustomerRegistered {
    private String customerId;
    private String email;
    private String fullName;
    private String phoneNumber;

    public CustomerRegistered() {}

    public CustomerRegistered(String customerId, String email, String fullName, String phoneNumber) {
        this.customerId = customerId;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

@EventType
class CustomerAddressUpdated {
    private String customerId;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;

    public CustomerAddressUpdated() {}

    public CustomerAddressUpdated(String customerId, String streetAddress, String city, 
                                 String postalCode, String country) {
        this.customerId = customerId;
        this.streetAddress = streetAddress;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}

@ReadModel
class Customer {
    private String id = "";
    private String fullName = "";
    private String email = "";
    private String phoneNumber = "";
    private String streetAddress = "";
    private String city = "";
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
    private String fullName = "";
    private String email = "";
    private String phoneNumber = "";
    private String streetAddress = "";
    private String city = "";
    private String postalCode = "";
    private String country = "";

    public CustomerDetails() {}

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
        
        List<AppendResult> results = store.getEventLog().appendMany(
            sampleCustomer.id, 
            Arrays.asList(registered, addressUpdated)
        );
        
        List<AppendResult> failures = results.stream()
            .filter(r -> !r.isSuccess())
            .collect(Collectors.toList());
            
        if (!failures.isEmpty()) {
            String violations = failures.stream()
                .flatMap(r -> r.getConstraintViolations().stream())
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
            System.out.println("[pii] Could not register " + sampleCustomer.fullName + ": " + violations);
            return;
        }
        
        long lastSeq = results.get(results.size() - 1).getSequenceNumber().getValue();
        System.out.println("[pii] Registered " + sampleCustomer.fullName + " (" + 
                          sampleCustomer.id + ") with PII events up to sequence " + lastSeq);
    }

    public static void showCustomerReadModel(IEventStore store) throws Exception {
        SampleCustomerData sampleCustomer = SampleCustomerData.instance;
        CustomerDetails customer = store.getReadModels().getInstanceByKey(
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

    private static String fmt(String label, String value, boolean isPii) {
        String displayValue = value.isEmpty() ? "(empty)" : value;
        String piiMarker = isPii ? "   [PII]" : "";
        return "  " + String.format("%-15s", label) + ": " + displayValue + piiMarker;
    }
}
