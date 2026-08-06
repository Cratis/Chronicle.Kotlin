// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections;

import io.cratis.chronicle.observation.EventSequence;
import io.cratis.chronicle.readModels.ReadModel;

/** A Java read model pointed at a sequence by the standalone annotation rather than a parameter. */
@ReadModel
@EventSequence("outbox")
public record JavaEventSequenceProjection(String orderId) {
}
