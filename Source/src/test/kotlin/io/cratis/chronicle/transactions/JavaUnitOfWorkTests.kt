// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.IEventStore
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JavaUnitOfWorkTests {
    @Test
    fun `java can begin inspect and find a unit of work by correlation id`() {
        assertTrue(JavaUnitOfWorkUsage.beginAndFind(UnitOfWorkManager(mockk<IEventStore>())))
    }
}
