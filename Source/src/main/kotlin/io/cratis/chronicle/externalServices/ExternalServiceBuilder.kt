// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.externalServices

import Cratis.Chronicle.Contracts.ExternalServices.Externalservices

class ExternalServiceBuilder : IExternalServiceBuilder {
    private val headers = mutableMapOf<String, String>()
    private val options = mutableMapOf<String, String>()
    private var type: Externalservices.ExternalServiceEndpointType = Externalservices.ExternalServiceEndpointType.Http
    private var url: String = ""
    private var authorization: Authorization? = null
    private var host: String = ""
    private var port: Int = 0
    private var database: String = ""
    private var username: String = ""
    private var password: String = ""

    override fun http(url: String): IExternalServiceBuilder {
        type = Externalservices.ExternalServiceEndpointType.Http
        this.url = url
        return this
    }

    override fun withBasicAuth(username: String, password: String): IExternalServiceBuilder {
        authorization = Authorization.Basic(username, password)
        return this
    }

    override fun withBearerToken(token: String): IExternalServiceBuilder {
        authorization = Authorization.Bearer(token)
        return this
    }

    override fun withOAuth(authority: String, clientId: String, clientSecret: String): IExternalServiceBuilder {
        authorization = Authorization.OAuth(authority, clientId, clientSecret)
        return this
    }

    override fun withHeader(key: String, value: String): IExternalServiceBuilder {
        headers[key] = value
        return this
    }

    override fun msSql(host: String, database: String, username: String, password: String, port: Int): IExternalServiceBuilder =
        configureDatabase(Externalservices.ExternalServiceEndpointType.MsSql, host, database, username, password, port)

    override fun postgreSql(host: String, database: String, username: String, password: String, port: Int): IExternalServiceBuilder =
        configureDatabase(Externalservices.ExternalServiceEndpointType.PostgreSql, host, database, username, password, port)

    override fun withOption(key: String, value: String): IExternalServiceBuilder {
        options[key] = value
        return this
    }

    /**
     * Builds the [Externalservices.ExternalServiceDefinition].
     *
     * @param id The identifier of the external service.
     * @param name The human-readable name of the external service.
     */
    fun build(id: String, name: String): Externalservices.ExternalServiceDefinition {
        val endpointBuilder = Externalservices.ExternalServiceEndpoint.newBuilder().setType(type)

        if (type == Externalservices.ExternalServiceEndpointType.Http) {
            val httpBuilder = Externalservices.HttpEndpointConfiguration.newBuilder()
                .setUrl(url)
                .putAllHeaders(headers)

            authorization?.let { auth ->
                val oneOfBuilder = Externalservices.OneOf_BasicAuthorization_BearerTokenAuthorization_OAuthAuthorization.newBuilder()
                when (auth) {
                    is Authorization.Basic -> oneOfBuilder.setValue0(
                        Externalservices.BasicAuthorization.newBuilder()
                            .setUsername(auth.username)
                            .setPassword(auth.password)
                            .build()
                    )
                    is Authorization.Bearer -> oneOfBuilder.setValue1(
                        Externalservices.BearerTokenAuthorization.newBuilder()
                            .setToken(auth.token)
                            .build()
                    )
                    is Authorization.OAuth -> oneOfBuilder.setValue2(
                        Externalservices.OAuthAuthorization.newBuilder()
                            .setAuthority(auth.authority)
                            .setClientId(auth.clientId)
                            .setClientSecret(auth.clientSecret)
                            .build()
                    )
                }
                httpBuilder.setAuthorization(oneOfBuilder.build())
            }

            endpointBuilder.setHttp(httpBuilder.build())
        } else {
            endpointBuilder.setDatabase(
                Externalservices.DatabaseEndpointConfiguration.newBuilder()
                    .setHost(host)
                    .setPort(port)
                    .setDatabase(database)
                    .setUsername(username)
                    .setPassword(password)
                    .putAllOptions(options)
                    .build()
            )
        }

        return Externalservices.ExternalServiceDefinition.newBuilder()
            .setId(id)
            .setName(name)
            .setEndpoint(endpointBuilder.build())
            .build()
    }

    private fun configureDatabase(
        type: Externalservices.ExternalServiceEndpointType,
        host: String,
        database: String,
        username: String,
        password: String,
        port: Int
    ): IExternalServiceBuilder {
        this.type = type
        this.host = host
        this.database = database
        this.username = username
        this.password = password
        this.port = port
        return this
    }

    private sealed class Authorization {
        data class Basic(val username: String, val password: String) : Authorization()
        data class Bearer(val token: String) : Authorization()
        data class OAuth(val authority: String, val clientId: String, val clientSecret: String) : Authorization()
    }
}
