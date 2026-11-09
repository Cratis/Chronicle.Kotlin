// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.events.EventType
import java.util.UUID

// The events of the Organization graph, the same ones the .NET client specs declare in
// Source/Clients/DotNET.Specs/Projections/Graphs/for_GraphDefinitionBuilder/given.

@EventType
data class PersonRegistered(val personId: UUID, val name: String)

@EventType
data class PersonRemoved(val personId: UUID)

@EventType
data class GroupCreated(val groupId: UUID, val name: String)

@EventType
data class MemberAdded(val personId: UUID, val groupId: UUID)

@EventType
data class MemberRemoved(val personId: UUID, val groupId: UUID)

@EventType
data class PlaceVisited(val personId: UUID, val placeId: UUID, val visitId: UUID)
