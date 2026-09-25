// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

/** The server rejected the client's contract descriptor during compatibility preflight. */
class ChronicleServerIncompatible(message: String) : IllegalStateException(message)
