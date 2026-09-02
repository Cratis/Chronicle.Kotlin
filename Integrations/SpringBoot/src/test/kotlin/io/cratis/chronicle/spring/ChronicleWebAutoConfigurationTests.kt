// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

class ChronicleWebAutoConfigurationTests {
    private val runner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ChronicleWebAutoConfiguration::class.java))

    @Test
    fun `web auto configuration contributes no ambient operation filters`() {
        runner.run { context ->
            assertThat(context.beanDefinitionNames)
                .doesNotContain(
                    "chronicleUnitOfWorkFilter",
                    "chronicleCausationFilter",
                    "chronicleIdentityFilter"
                )
        }
    }
}
