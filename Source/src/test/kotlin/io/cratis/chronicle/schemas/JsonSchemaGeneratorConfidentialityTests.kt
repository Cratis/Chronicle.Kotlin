// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import com.google.gson.Gson
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.concepts.EventSourceId
import io.cratis.chronicle.confidentiality.Encrypted
import io.cratis.chronicle.confidentiality.EncryptedNotSupportedOnEventSourceId
import io.cratis.chronicle.confidentiality.EncryptionScope
import io.cratis.chronicle.confidentiality.PiiAndEncryptedCombinedNotSupported
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class ApiKey(@Encrypted val value: String)

private data class PartnerIntegration(
    val partnerName: String,
    @Encrypted(description = "Partner API key") val apiKey: String
)

@Encrypted
private data class WebhookSecret(override val value: String) : ConceptAs<String>

private data class Configuration(val webhookSecret: WebhookSecret)

@Encrypted
private data class LicenseToken(override val value: String) : ConceptAs<String>

private data class FullyEncryptedClass(val value: String, val otherValue: String)

private data class ContactDetails(val phone: String, val fax: String)

private data class Vendor(val name: String, @Encrypted(description = "Vendor contact information") val contact: ContactDetails)

private data class Session(val tokens: List<LicenseToken>)

private data class ConflictedValue(@Pii @Encrypted val value: String)

@Pii
private data class MixedClass(@Encrypted val value: String)

@Encrypted
private data class EncryptedEmployeeId(override val value: String) : EventSourceId

private data class EventWithEncryptedEventSourceId(val employeeId: EncryptedEmployeeId, val name: String)

@Encrypted(scope = EncryptionScope.Namespace)
private data class WebhookSecretScoped(override val value: String) : ConceptAs<String>

@Encrypted(scope = EncryptionScope.Global)
private data class LicenseTokenScoped(override val value: String) : ConceptAs<String>

private data class ScopedSecrets(val namespaceSecret: WebhookSecretScoped, val globalSecret: LicenseTokenScoped)

class JsonSchemaGeneratorConfidentialityTests {
    private val gson = Gson()

    @Test
    fun `a property without Encrypted carries no security metadata`() {
        val properties = propertiesOf(PartnerIntegration::class)
        val partnerName = properties["partnerName"] as Map<*, *>
        assertFalse(partnerName.containsKey("security"))
    }

    @Test
    fun `a property annotated with Encrypted carries EncryptedSubject security metadata by default`() {
        val properties = propertiesOf(PartnerIntegration::class)
        val apiKey = properties["apiKey"] as Map<*, *>
        val security = securityOf(apiKey)
        assertEquals(1, security.size)
        assertEquals("EncryptedSubject", security[0]["metadataType"])
        assertEquals("Partner API key", security[0]["details"])
    }

    @Test
    fun `a property annotated with Encrypted carries no compliance metadata`() {
        val properties = propertiesOf(PartnerIntegration::class)
        val apiKey = properties["apiKey"] as Map<*, *>
        assertFalse(apiKey.containsKey("compliance"))
    }

    @Test
    fun `a property whose declared type is Encrypted-marked also carries security metadata`() {
        val properties = propertiesOf(Configuration::class)
        val webhookSecret = properties["webhookSecret"] as Map<*, *>
        val security = securityOf(webhookSecret)
        assertEquals(1, security.size)
        assertEquals("EncryptedSubject", security[0]["metadataType"])
    }

    @Test
    fun `an Encrypted concept scoped to Namespace carries EncryptedNamespace security metadata`() {
        val properties = propertiesOf(ScopedSecrets::class)
        val namespaceSecret = properties["namespaceSecret"] as Map<*, *>
        assertEquals("EncryptedNamespace", securityOf(namespaceSecret)[0]["metadataType"])
    }

    @Test
    fun `an Encrypted concept scoped to Global carries EncryptedGlobal security metadata`() {
        val properties = propertiesOf(ScopedSecrets::class)
        val globalSecret = properties["globalSecret"] as Map<*, *>
        assertEquals("EncryptedGlobal", securityOf(globalSecret)[0]["metadataType"])
    }

    @Test
    fun `a class-level Encrypted annotation cascades to every leaf property instead of tagging the container`() {
        val schema = parse(FullyEncryptedClass::class)
        assertFalse(schema.containsKey("security"))
    }

    @Test
    fun `a constructor parameter annotated with Encrypted carries EncryptedSubject security metadata`() {
        val properties = propertiesOf(ApiKey::class)
        val value = properties["value"] as Map<*, *>
        assertEquals(1, securityOf(value).size)
        assertEquals("EncryptedSubject", securityOf(value)[0]["metadataType"])
    }

    @Test
    fun `an Encrypted-marked element type carries security metadata on the array's items`() {
        val properties = propertiesOf(Session::class)
        val tokens = properties["tokens"] as Map<*, *>
        val items = tokens["items"] as Map<*, *>
        assertEquals(1, securityOf(items).size)
        assertEquals("EncryptedSubject", securityOf(items)[0]["metadataType"])
    }

    @Test
    fun `an Encrypted-marked nested value object descends to its own leaves, not the container`() {
        val properties = propertiesOf(Vendor::class)
        val contact = properties["contact"] as Map<*, *>
        assertFalse(contact.containsKey("security"))

        @Suppress("UNCHECKED_CAST")
        val contactProperties = contact["properties"] as Map<String, Any?>
        val phone = contactProperties["phone"] as Map<*, *>
        val fax = contactProperties["fax"] as Map<*, *>
        assertEquals(1, securityOf(phone).size)
        assertEquals(1, securityOf(fax).size)
        assertTrue(securityOf(phone)[0]["details"] == "Vendor contact information")
    }

    @Test
    fun `an Encrypted annotation on an EventSourceId-typed concept is rejected`() {
        assertThrows(EncryptedNotSupportedOnEventSourceId::class.java) {
            JsonSchemaGenerator.generate(EventWithEncryptedEventSourceId::class)
        }
    }

    @Test
    fun `a property carrying both Pii and Encrypted is rejected`() {
        assertThrows(PiiAndEncryptedCombinedNotSupported::class.java) {
            JsonSchemaGenerator.generate(ConflictedValue::class)
        }
    }

    @Test
    fun `a class-level Pii combined with a property-level Encrypted on the same property is rejected`() {
        assertThrows(PiiAndEncryptedCombinedNotSupported::class.java) {
            JsonSchemaGenerator.generate(MixedClass::class)
        }
    }

    private fun parse(cls: KClass<*>): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return gson.fromJson(JsonSchemaGenerator.generate(cls), Map::class.java) as Map<String, Any?>
    }

    private fun propertiesOf(cls: KClass<*>): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return parse(cls)["properties"] as Map<String, Any?>
    }

    private fun securityOf(schemaNode: Map<*, *>): List<Map<String, String>> {
        @Suppress("UNCHECKED_CAST")
        return (schemaNode["security"] as? List<Map<String, String>>).orEmpty()
    }
}
