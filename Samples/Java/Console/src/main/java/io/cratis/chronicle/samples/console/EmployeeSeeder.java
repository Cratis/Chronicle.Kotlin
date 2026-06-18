// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.seeding.ICanSeedEvents;
import io.cratis.chronicle.seeding.IEventSeedingBuilder;
import io.cratis.chronicle.seeding.Seeder;

import java.util.Arrays;
import java.util.List;

@Seeder
public class EmployeeSeeder implements ICanSeedEvents {
    private static final List<String> seedTitles = Arrays.asList(
        "Software Engineer", "Senior Engineer", "Principal Engineer"
    );
    
    private static final class Address {
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
    
    private static final List<Address> seedAddresses = Arrays.asList(
        new Address("221B Baker Street", "London", "NW1 6XE", "UK"),
        new Address("1600 Amphitheatre Parkway", "Mountain View", "94043", "USA"),
        new Address("1 Infinite Loop", "Cupertino", "95014", "USA")
    );

    @Override
    public void seed(IEventSeedingBuilder builder) {
        List<Person> employees = Employees.employees;
        for (int index = 0; index < employees.size(); index++) {
            Person person = employees.get(index);
            String title = seedTitles.get(index % seedTitles.size());
            Address addr = seedAddresses.get(index % seedAddresses.size());
            
            builder.forEventSource(
                person.getId(),
                Arrays.asList(
                    new EmployeeHired(person.getFirstName(), person.getLastName(), title),
                    new EmployeeEmailSet(Employees.emailFor(person)),
                    new EmployeeAddressSet(addr.address, addr.city, addr.zipCode, addr.country)
                )
            );
        }
    }
}
