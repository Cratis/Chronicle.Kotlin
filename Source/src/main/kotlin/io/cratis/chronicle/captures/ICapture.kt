// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

/**
 * A capture an application owns, declared in code so it is discovered and saved like every other
 * artifact.
 *
 * A capture pulls something that is not Chronicle - an HTTP API, a webhook someone else posts to -
 * and appends what it finds as events. Its shape is a Capture Declaration Language document, which
 * is what the kernel understands and what it holds on to.
 *
 * Implement this and the capture is saved with the kernel on connect and started, so the source is
 * live as soon as the application is:
 *
 * ```kotlin
 * class ExchangeRates : ICapture {
 *     override val id = "exchange-rates"
 *
 *     override val declaration = """
 *         capture ExchangeRates
 *             from api "https://api.example.com/rates" every 5 minutes
 *                 append RateObserved
 *                     set currency to base
 *                     set rate to value
 *     """
 * }
 * ```
 *
 * Prefer [io.cratis.chronicle.captures.ICapturesService] directly when the declaration is not known
 * at build time - one being written in an editor, or read from configuration.
 */
interface ICapture {
    /** The identifier the capture is held under. Stable: renaming it creates a second capture. */
    val id: String

    /** The Capture Declaration Language document defining what the capture pulls in and appends. */
    val declaration: String

    /**
     * Whether the capture should be started once saved.
     *
     * Leave this alone unless the capture should be saved but held - an environment where the source
     * is not reachable, say, or one where starting it is an operator's decision.
     */
    val startOnRegistration: Boolean get() = true
}
