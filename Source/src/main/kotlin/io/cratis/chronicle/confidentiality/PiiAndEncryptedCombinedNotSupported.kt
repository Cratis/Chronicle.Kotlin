// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.confidentiality

/**
 * Thrown when a property, or the type it resolves metadata from, carries both
 * [io.cratis.chronicle.compliance.Pii] and [Encrypted].
 *
 * This is not merely redundant - it corrupts the value. The kernel applies every matching handler
 * for a property in sequence, so a value marked both ways is encrypted first under the PII key and
 * then again under the Encrypted key; releasing it decrypts with the wrong key against ciphertext,
 * which fails loudly (a padding/authentication error) rather than returning a wrong value. A value
 * needs exactly one protection: `@Pii` when it is personal data with a lawful basis for erasure,
 * `@Encrypted` when it is an operational secret with none.
 *
 * @param property The name of the property (or type) carrying both annotations.
 */
class PiiAndEncryptedCombinedNotSupported(property: String) : Exception(
    "'$property' carries both @Pii and @Encrypted. A value needs exactly one protection - combining them " +
        "would encrypt it twice, under two different keys, and it cannot be released correctly. Choose @Pii " +
        "for personal data with a lawful basis for erasure, or @Encrypted for an operational secret with none."
)
