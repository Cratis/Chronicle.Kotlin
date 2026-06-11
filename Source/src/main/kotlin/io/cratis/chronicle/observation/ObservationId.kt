// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.util.UUID

object ObservationId {
    fun generate(): String = UUID.randomUUID().toString()
}
