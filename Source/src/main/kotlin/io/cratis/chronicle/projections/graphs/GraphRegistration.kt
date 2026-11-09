// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import Cratis.Chronicle.Contracts.ReadModels.Readmodels

/**
 * What one graph registers with the kernel: the read model it is stored as and the projection that fills it.
 *
 * @property graphName The name of the graph.
 * @property readModel The read model definition, carrying the schema with the graph annotations.
 * @property projection The projection definition, carrying the graph mappings.
 */
internal data class GraphRegistration(
    val graphName: String,
    val readModel: Readmodels.ReadModelDefinition,
    val projection: ProjectionsOuterClass.ProjectionDefinition
)
