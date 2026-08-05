// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/**
 * A promotion has been recorded in the shared HR audit log — a reactor side effect of {@link EmployeePromoted},
 * appended to a constant event source rather than the promoted employee's own stream.
 */
@EventType
record PromotionAudited(String employeeId, String newTitle) {}
