// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison;

import java.time.Instant;

/** Immutable Java event content used by both comparison engines. */
public record ComparisonRecord(String name, ComparisonStatus status, Instant at) {}
