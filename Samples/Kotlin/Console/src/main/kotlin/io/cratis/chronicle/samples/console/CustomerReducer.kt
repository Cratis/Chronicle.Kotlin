// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.observation.Reducer

@Reducer
class CustomerReducer {

    fun customerRegistered(event: CustomerRegistered): CustomerDetails {
        return CustomerDetails(
            id = event.customerId,
            fullName = event.fullName,
            email = event.email,
            phoneNumber = event.phoneNumber
        )
    }

    fun customerAddressUpdated(event: CustomerAddressUpdated, state: CustomerDetails?): CustomerDetails {
        return (state ?: CustomerDetails()).copy(
            streetAddress = event.streetAddress,
            city = event.city,
            postalCode = event.postalCode,
            country = event.country
        )
    }
}
