// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

/**
 * Thrown when a `chronicle+srv://` connection string's DNS SRV lookup returns no server records.
 */
class ChronicleSrvResolutionException(host: String) :
    RuntimeException("DNS SRV lookup for '_chronicle._tcp.$host' returned no records.")
