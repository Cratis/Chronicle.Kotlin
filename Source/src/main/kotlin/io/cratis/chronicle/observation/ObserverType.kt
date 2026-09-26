// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * What kind of observer an [ObserverInformation] describes.
 */
enum class ObserverType {
    /** The type of the observer is not known. */
    Unknown,

    /** The observer is a reactor. */
    Reactor,

    /** The observer is a projection. */
    Projection,

    /** The observer is a reducer. */
    Reducer,

    /** The observer is driven by something outside Chronicle. */
    External
}
