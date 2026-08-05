// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

import io.cratis.chronicle.readModels.ReadModel;

/** A read model declared in Java. */
@ReadModel
public record JavaBookState(String title, boolean present) {
}
