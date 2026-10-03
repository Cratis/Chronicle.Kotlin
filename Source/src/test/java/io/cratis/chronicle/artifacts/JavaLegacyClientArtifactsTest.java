// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts;

import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Java implementation of {@link IClientArtifacts} written before event sources existed: it implements only
 * the members the interface had then. It has to keep compiling, linking and behaving as it did.
 */
class JavaLegacyClientArtifactsTest {

    /** Implements exactly the previous release's members and nothing about event sources. */
    static final class LegacyArtifacts implements IClientArtifacts {
        @Override public List<kotlin.reflect.KClass<?>> getEventTypes() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getEventTypeMigrations() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getReadModels() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getProjections() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getModelBoundProjections() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getReactors() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getReducers() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getConstraints() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getEventSeeders() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getWebhooks() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getCaptures() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getReactorMiddlewares() { return List.of(); }
        @Override public List<kotlin.reflect.KClass<?>> getReactorArgumentResolvers() { return List.of(); }
    }

    @Test
    void anImplementationWithoutEventSourcesReportsNoDefinitions() {
        var artifacts = new LegacyArtifacts();

        assertTrue(ClientArtifactsEventSources.getEventSources(artifacts).isEmpty());
    }

    @Test
    void theBuiltInImplementationsReportTheirEventSources() {
        assertTrue(ClientArtifactsEventSources.getEventSources(new KnownClientArtifacts(List.of())).isEmpty());
    }

    @Test
    void theShortPositionalFormsStillBind() {
        var options = new AppendOptions(UUID.randomUUID());
        var event = new EventForEventSourceId("id", "event");

        assertNull(options.getEventSource());
        assertNull(event.getEventSource());
        assertEquals("id", event.getEventSourceId());
    }
}
