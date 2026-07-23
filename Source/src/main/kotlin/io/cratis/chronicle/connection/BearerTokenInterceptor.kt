// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import kotlinx.coroutines.runBlocking

private val AUTHORIZATION_HEADER: Metadata.Key<String> =
    Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

/**
 * gRPC client interceptor that attaches a Bearer token to every outbound call.
 *
 * The token is obtained synchronously from [tokenProvider] before each call so it is
 * always fresh. The provider is responsible for caching and renewal. Attaching the token
 * per call rather than at channel build time means an expiring token never invalidates
 * the channel: streams opened while a token was valid stay authenticated, and every new
 * call carries a current token. When no token can be obtained the call proceeds without
 * auth and fails with the server's rejection — recovery is the session machinery's job.
 */
class BearerTokenInterceptor(private val tokenProvider: ITokenProvider) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val token = runBlocking { tokenProvider.getAccessToken() }

        return if (token != null) {
            object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)
            ) {
                override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                    headers.put(AUTHORIZATION_HEADER, "Bearer $token")
                    super.start(responseListener, headers)
                }
            }
        } else {
            next.newCall(method, callOptions)
        }
    }
}
