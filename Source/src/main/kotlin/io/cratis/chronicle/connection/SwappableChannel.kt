// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ManagedChannel
import io.grpc.MethodDescriptor

/**
 * A [io.grpc.Channel] whose underlying [ManagedChannel] can be replaced without touching
 * the stubs holding it.
 *
 * Stubs bind to the channel they were created with for their whole life, so rebuilding a
 * connection — redialing after a session drop to pick up a changed DNS record or a
 * replaced host — would otherwise invalidate every stub in the client. Calls started
 * before a [swap] finish on the channel they started on; calls started after it use the
 * replacement.
 */
class SwappableChannel(initial: ManagedChannel) : io.grpc.Channel() {

    @Volatile
    private var delegate: ManagedChannel = initial

    /** The managed channel currently carrying calls. */
    val current: ManagedChannel get() = delegate

    /** Replaces the underlying channel and returns the one it replaced. */
    fun swap(replacement: ManagedChannel): ManagedChannel {
        val previous = delegate
        delegate = replacement
        return previous
    }

    override fun <ReqT, RespT> newCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions
    ): ClientCall<ReqT, RespT> = delegate.newCall(method, callOptions)

    override fun authority(): String = delegate.authority()
}
