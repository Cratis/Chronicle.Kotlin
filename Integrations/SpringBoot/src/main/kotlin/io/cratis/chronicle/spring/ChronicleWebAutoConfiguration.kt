// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication

/**
 * Web marker configuration.
 *
 * Chronicle deliberately installs no identity, causation, correlation, or transaction servlet
 * filters. Controllers construct an [io.cratis.chronicle.OperationContext] from their request and
 * pass it to operations explicitly; direct appends never enroll in request-global work.
 */
@AutoConfiguration(after = [ChronicleAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ChronicleWebAutoConfiguration
