// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.confidentiality

/**
 * Marks a property, constructor parameter, field, or type as needing plain-confidentiality
 * encryption at rest - a security measure, not a compliance one.
 *
 * Use `@Encrypted` for an operational secret that has no data subject and no lawful basis for
 * erasure - an API key, a webhook signing secret, a partner credential. Use
 * [io.cratis.chronicle.compliance.Pii] instead when the value is personal data about a natural
 * person: only `@Pii` encrypts a value *and* enrolls it in GDPR right-to-erasure. Marking a secret
 * `@Pii` would make it erasable on a request that was never about it; marking personal data
 * `@Encrypted` would encrypt it but never erase it. The two are not interchangeable, and this
 * annotation's key is never provisioned under the same identity a `@Pii` value for the same subject
 * uses.
 *
 * `@Encrypted` cannot be applied to an [io.cratis.chronicle.concepts.EventSourceId] - see
 * [EncryptedNotSupportedOnEventSourceId] for why. A property or type cannot carry both `@Pii` and
 * `@Encrypted` - see [PiiAndEncryptedCombinedNotSupported] for why.
 *
 * @property scope The [EncryptionScope] the key is provisioned under - defaults to [EncryptionScope.Subject].
 * @property description Optional description of why the value needs encryption.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Encrypted(val scope: EncryptionScope = EncryptionScope.Subject, val description: String = "")
