// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

/** A Java reducer relying entirely on the annotation's default values. */
@Reducer
public class JavaDefaultReducer {

    public JavaBookState bookAdded(JavaBookAdded event, JavaBookState state) {
        return new JavaBookState(event.title(), true);
    }
}
