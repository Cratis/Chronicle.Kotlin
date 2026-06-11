// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.transactions.UnitOfWorkManager

interface IEventStore {
    val name: String
    val namespace: String
    val eventLog: IEventLog
    val reactors: IReactorsService
    val reducers: IReducersService
    val projections: IProjectionsService
    val constraints: IConstraintsService
    val seeding: IEventSeedingService
    val readModels: IReadModelsService
    val unitOfWorkManager: UnitOfWorkManager
}
