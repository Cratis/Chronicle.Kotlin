// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.externalServices

/**
 * Defines a fluent builder for configuring an external service.
 */
interface IExternalServiceBuilder {
    /**
     * Configures the service as an HTTP endpoint.
     *
     * @param url The base URL of the endpoint.
     */
    fun http(url: String): IExternalServiceBuilder

    /**
     * Configures basic authentication for an HTTP endpoint.
     *
     * @param username The username.
     * @param password The password.
     */
    fun withBasicAuth(username: String, password: String): IExternalServiceBuilder

    /**
     * Configures bearer token authentication for an HTTP endpoint.
     *
     * @param token The bearer token.
     */
    fun withBearerToken(token: String): IExternalServiceBuilder

    /**
     * Configures OAuth authentication for an HTTP endpoint.
     *
     * @param authority The OAuth authority.
     * @param clientId The OAuth client id.
     * @param clientSecret The OAuth client secret.
     */
    fun withOAuth(authority: String, clientId: String, clientSecret: String): IExternalServiceBuilder

    /**
     * Adds a header to send with every HTTP request.
     *
     * @param key The header key.
     * @param value The header value.
     */
    fun withHeader(key: String, value: String): IExternalServiceBuilder

    /**
     * Configures the service as a Microsoft SQL Server database endpoint.
     *
     * @param host The database host.
     * @param database The database name.
     * @param username The username used to connect.
     * @param password The password used to connect.
     * @param port The database port. Leave as 0 to use the provider default.
     */
    fun msSql(host: String, database: String, username: String, password: String, port: Int = 0): IExternalServiceBuilder

    /**
     * Configures the service as a PostgreSQL database endpoint.
     *
     * @param host The database host.
     * @param database The database name.
     * @param username The username used to connect.
     * @param password The password used to connect.
     * @param port The database port. Leave as 0 to use the provider default.
     */
    fun postgreSql(host: String, database: String, username: String, password: String, port: Int = 0): IExternalServiceBuilder

    /**
     * Adds a provider-specific option to a database endpoint's connection string.
     *
     * @param key The option key.
     * @param value The option value.
     */
    fun withOption(key: String, value: String): IExternalServiceBuilder
}
