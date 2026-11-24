// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * The kernel accepted a projection registration but could not apply it.
 *
 * @param registrant Who the registration was made as.
 * @param failure Why the kernel could not apply it.
 */
class ProjectionRegistrationNotApplied(val registrant: String, val failure: String) :
    RuntimeException("The kernel accepted the projection registration of '$registrant' but could not apply it: $failure")
