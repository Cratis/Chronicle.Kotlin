// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java;

import io.cratis.chronicle.readModels.IReadModelsService;

import java.util.List;
import java.util.concurrent.Flow;

/** Compiles the Java call site, including its inferred element type and subscription. */
public final class JavaMaterializedPublisherUsage {
    private JavaMaterializedPublisherUsage() {
    }

    public record Book(String title) {
    }

    public static Flow.Publisher<List<Book>> observe(IReadModelsService readModels, Flow.Subscriber<List<Book>> subscriber) {
        Flow.Publisher<List<Book>> publisher = ReadModelsJavaBridge.observeMaterializedInstancesPublisher(
            readModels, Book.class, 0, 50);
        publisher.subscribe(subscriber);
        return publisher;
    }
}
