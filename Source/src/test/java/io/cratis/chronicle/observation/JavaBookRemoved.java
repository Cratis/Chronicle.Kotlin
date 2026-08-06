// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

import io.cratis.chronicle.events.EventType;

/** A second Java event type, so Java reactors can carry more than one handler. */
@EventType
public record JavaBookRemoved(String title) {
}
