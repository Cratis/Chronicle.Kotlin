// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.artifacts.ArtifactActivationFailed
import io.cratis.chronicle.artifacts.IArtifactActivator
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

/**
 * Activates Chronicle artifacts through the Spring container, so a reactor, reducer, constraint or
 * seeder is an ordinary Spring component with ordinary constructor injection:
 *
 * ```kotlin
 * @Reactor
 * class WelcomePackage(private val mailer: Mailer) {
 *     fun employeeHired(event: EmployeeHired) = mailer.send(event.email, "Welcome!")
 * }
 * ```
 *
 * An artifact declared as a bean — `@Component`, `@Service`, an `@Bean` method — is used as it stands,
 * with its own scope and lifecycle. One that is not is still constructed with its dependencies
 * injected, so annotating artifacts as components is a choice rather than an obligation.
 *
 * @param applicationContext The context to resolve and construct artifacts through.
 */
class SpringArtifactActivator(private val applicationContext: ApplicationContext) : IArtifactActivator {
    override fun activate(type: KClass<*>): Any =
        existingBean(type) ?: try {
            applicationContext.autowireCapableBeanFactory.createBean(type.java)
        } catch (cause: Throwable) {
            throw ArtifactActivationFailed(type, cause)
        }

    /**
     * The artifact as an already-defined bean, if there is exactly one.
     *
     * Ambiguity is treated as "not declared" rather than as an error: the artifact is then constructed
     * fresh, which is what would have happened had no bean been declared at all.
     */
    private fun existingBean(type: KClass<*>): Any? =
        applicationContext.getBeanProvider(type.java).getIfUnique()
}
