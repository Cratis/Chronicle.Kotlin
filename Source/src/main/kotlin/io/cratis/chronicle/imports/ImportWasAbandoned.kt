// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/**
 * Thrown when events are added to, or the completion is asked of, an import that was abandoned.
 *
 * @property importId The abandoned import.
 */
class ImportWasAbandoned(val importId: ImportId) : IllegalStateException(
    "Import '$importId' was abandoned; no more events are sent to it and it cannot be completed."
)
