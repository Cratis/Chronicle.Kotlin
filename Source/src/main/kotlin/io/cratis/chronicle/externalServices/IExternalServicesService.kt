// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.externalServices

interface IExternalServicesService {
    /**
     * Registers an external service.
     *
     * @param name The name of the external service. The name is also used as its identifier.
     * @param configure The callback for configuring the external service.
     */
    suspend fun register(name: String, configure: (IExternalServiceBuilder) -> Unit)
}
