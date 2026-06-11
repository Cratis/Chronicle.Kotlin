// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.compliance

interface IComplianceService {
    suspend fun release(subject: String, schema: String = "{}", payload: String = "{}"): String
}
