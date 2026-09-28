// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java.for_ReadModelsJavaBridge.given

import io.cratis.chronicle.readModels.IMaterializedReadModels
import io.cratis.chronicle.readModels.IReadModelsService
import io.mockk.every
import io.mockk.mockk

internal data class Book(val title: String)

internal class AnObservation {
    val materialized = mockk<IMaterializedReadModels>()
    val service = mockk<IReadModelsService>()

    init {
        every { service.materialized } returns materialized
    }
}
