// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.artifacts.ArtifactActivationFailed
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class SpringArtifactActivatorTests {

    class Mailer

    /** Not declared as a bean, but still gets its dependency injected. */
    class NotABean(val mailer: Mailer)

    /** Declared as a bean, so the one the container already has is the one that is used. */
    class DeclaredAsABean

    /** Nothing can supply this. */
    class Unsatisfiable(val missing: java.io.File)

    @Configuration(proxyBeanMethods = false)
    class Beans {
        @Bean fun mailer(): Mailer = Mailer()

        @Bean fun declared(): DeclaredAsABean = DeclaredAsABean()
    }

    private val context = AnnotationConfigApplicationContext(Beans::class.java)
    private val activator = SpringArtifactActivator(context)

    @Test
    fun `hands back the bean an application already declared`() {
        val declared = context.getBean(DeclaredAsABean::class.java)

        assertThat(activator.activate(DeclaredAsABean::class)).isSameAs(declared)
    }

    @Test
    fun `constructs an artifact that is not a bean, injecting what it needs`() {
        val activated = activator.activate(NotABean::class)

        assertThat(activated).isInstanceOf(NotABean::class.java)
        assertThat((activated as NotABean).mailer).isSameAs(context.getBean(Mailer::class.java))
    }

    @Test
    fun `explains itself when the container cannot satisfy the artifact`() {
        assertThatThrownBy { activator.activate(Unsatisfiable::class) }
            .isInstanceOf(ArtifactActivationFailed::class.java)
            .hasMessageContaining("Unsatisfiable")
    }
}
