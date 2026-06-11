// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class Employee(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val title: String = "",
    val address: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = ""
)
