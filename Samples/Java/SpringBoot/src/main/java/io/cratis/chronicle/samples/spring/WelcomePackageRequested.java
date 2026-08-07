// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring;

import io.cratis.chronicle.events.EventType;

/** A welcome package has been requested for a newly hired employee. */
@EventType
public record WelcomePackageRequested(String employeeId) {}
