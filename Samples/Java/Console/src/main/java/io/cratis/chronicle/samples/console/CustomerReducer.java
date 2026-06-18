// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.observation.Reducer;

@Reducer
public class CustomerReducer {

    public CustomerDetails customerRegistered(CustomerRegistered event) {
        return new CustomerDetails(
            event.customerId(),
            event.fullName(),
            event.email(),
            event.phoneNumber(),
            "",
            "",
            "",
            ""
        );
    }

    public CustomerDetails customerAddressUpdated(CustomerAddressUpdated event, CustomerDetails state) {
        CustomerDetails current = state != null ? state : new CustomerDetails();
        return new CustomerDetails(
            current.getId(),
            current.getFullName(),
            current.getEmail(),
            current.getPhoneNumber(),
            event.streetAddress(),
            event.city(),
            event.postalCode(),
            event.country()
        );
    }
}
