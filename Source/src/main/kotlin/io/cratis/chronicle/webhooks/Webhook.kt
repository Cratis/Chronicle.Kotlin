// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

/**
 * Marks a class as a discoverable Chronicle webhook definition.
 *
 * @property id Explicit identifier. Defaults to the class's simple name.
 * @property targetUrl The URL to send events to.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Webhook(val id: String = "", val targetUrl: String)
