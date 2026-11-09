// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/** Thrown when the event sequence, or the kernel it talks to, cannot import events. */
class ImportsNotSupported(message: String = "This event sequence does not support importing events.") :
    UnsupportedOperationException(message)
