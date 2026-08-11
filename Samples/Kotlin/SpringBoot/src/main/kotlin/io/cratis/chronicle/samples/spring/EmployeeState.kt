// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring

import io.cratis.chronicle.readModels.ReadModel

/** What an employee looks like right now, folded together from everything that has happened to them. */
@ReadModel
data class EmployeeState(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val title: String = "",
    val email: String = ""
)
