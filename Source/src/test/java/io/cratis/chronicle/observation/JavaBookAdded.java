// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

import io.cratis.chronicle.events.EventType;

/** An event type declared in Java, used to prove Java annotation defaults work. */
@EventType
public record JavaBookAdded(String title) {
}
