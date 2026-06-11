// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

private val seedTitles = listOf("Software Engineer", "Senior Engineer", "Principal Engineer")
private val seedAddresses = listOf(
    Triple("221B Baker Street",         "London",        "NW1 6XE"  ) to "UK",
    Triple("1600 Amphitheatre Parkway", "Mountain View", "94043"    ) to "USA",
    Triple("1 Infinite Loop",           "Cupertino",     "95014"    ) to "USA"
)

@Seeder
class EmployeeSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        employees.forEachIndexed { index, person ->
            val title = seedTitles[index % seedTitles.size]
            val (addr, country) = seedAddresses[index % seedAddresses.size]
            val (address, city, zipCode) = addr
            builder.forEventSource(
                person.id,
                listOf(
                    EmployeeHired(person.firstName, person.lastName, title),
                    EmployeeEmailSet(emailFor(person)),
                    EmployeeAddressSet(address, city, zipCode, country)
                )
            )
        }
    }
}
