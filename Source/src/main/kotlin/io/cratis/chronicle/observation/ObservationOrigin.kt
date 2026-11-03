// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/** The path a batch of events took to reach an observer. */
enum class ObservationOrigin {
    /** The origin is not known, for example because the kernel does not report it. */
    Unknown,

    /** The events were appended now and delivered to an observer that is up to date. */
    Live,

    /** The events were read from storage for an observer that is behind. */
    CatchUp,

    /** The events were read from storage for an observer that was rewound. */
    Replay,

    /** The events were read from storage to recover a failed or deferred partition. */
    Retry,
}
