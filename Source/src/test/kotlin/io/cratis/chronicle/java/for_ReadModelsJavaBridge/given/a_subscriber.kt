// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java.for_ReadModelsJavaBridge.given

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

internal class RecordingSubscriber<T> : Flow.Subscriber<T> {
    private val subscribed = CompletableFuture<Flow.Subscription>()
    val values = LinkedBlockingQueue<T>()
    val failed = CompletableFuture<Throwable>()
    val completed = CompletableFuture<Unit>()

    val subscription: Flow.Subscription get() = subscribed.get(2, TimeUnit.SECONDS)

    override fun onSubscribe(subscription: Flow.Subscription) {
        subscribed.complete(subscription)
    }

    override fun onNext(item: T) {
        values.add(item)
    }

    override fun onError(throwable: Throwable) {
        failed.complete(throwable)
    }

    override fun onComplete() {
        completed.complete(Unit)
    }
}
