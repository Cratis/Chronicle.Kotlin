// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

/**
 * Exception that gets thrown when no unit of work has been started.
 */
class NoUnitOfWorkHasBeenStarted : Exception("No unit of work has been started")
