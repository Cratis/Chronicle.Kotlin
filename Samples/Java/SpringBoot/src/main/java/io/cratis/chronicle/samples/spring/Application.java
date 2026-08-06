// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A Spring Boot application backed by Chronicle, in Java.
 *
 * There is no Chronicle setup anywhere in this sample. The starter connects, finds every event type,
 * read model, reducer, reactor and constraint in this package, and registers them with the kernel
 * before the first request is served.
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
