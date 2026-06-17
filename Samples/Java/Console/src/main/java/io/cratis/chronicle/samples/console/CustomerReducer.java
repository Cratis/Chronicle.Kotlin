// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.observation.Reducer;

@Reducer
public class CustomerReducer {

    public CustomerDetails customerRegistered(CustomerRegistered event) {
        CustomerDetails details = new CustomerDetails();
        details.setId(event.getCustomerId());
        details.setFullName(event.getFullName());
        details.setEmail(event.getEmail());
        details.setPhoneNumber(event.getPhoneNumber());
        return details;
    }

    public CustomerDetails customerAddressUpdated(CustomerAddressUpdated event, CustomerDetails state) {
        CustomerDetails result = state != null ? state : new CustomerDetails();
        result.setStreetAddress(event.getStreetAddress());
        result.setCity(event.getCity());
        result.setPostalCode(event.getPostalCode());
        result.setCountry(event.getCountry());
        return result;
    }
}
