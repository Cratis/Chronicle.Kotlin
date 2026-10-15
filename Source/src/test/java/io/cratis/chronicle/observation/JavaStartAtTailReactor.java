// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

/** A Java reactor that begins at the tail of the event sequence when it is first registered. */
@Reactor
@StartAtTail
public class JavaStartAtTailReactor {

    public void bookAdded(JavaBookAdded event) {
    }
}
