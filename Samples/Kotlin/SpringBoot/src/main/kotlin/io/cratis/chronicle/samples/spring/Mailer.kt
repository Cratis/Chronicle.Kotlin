// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/** An ordinary Spring bean, injected into a reactor to show that artifacts take dependencies like anything else. */
@Service
class Mailer {
    private val logger = LoggerFactory.getLogger(Mailer::class.java)

    fun send(to: String, subject: String) = logger.info("Mail to {}: {}", to, subject)
}
