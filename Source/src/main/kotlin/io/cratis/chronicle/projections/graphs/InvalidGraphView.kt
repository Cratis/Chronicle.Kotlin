// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when a view declared with [ViewOf] breaks a rule of graph views.
 *
 * Every rule has an exception type of its own and a stable [reason] - the name of the rule, such as
 * `ViewRootNotANode` - that is the same in every Chronicle client.
 *
 * @property reason The stable name of the rule that was broken.
 * @property view The view class that breaks it.
 */
abstract class InvalidGraphView(val reason: String, val view: KClass<*>, message: String) : IllegalArgumentException(message)
