// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** An ordinary Spring bean, injected into a reactor to show that artifacts take dependencies like anything else. */
@Service
public class Mailer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Mailer.class);

    public void send(String to, String subject) {
        LOGGER.info("Mail to {}: {}", to, subject);
    }
}
