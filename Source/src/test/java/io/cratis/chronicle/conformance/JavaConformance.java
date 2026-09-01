// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.conformance;

import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.auditing.Causation;
import io.cratis.chronicle.auditing.CausationType;
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;
import io.cratis.chronicle.concepts.EventSourceId;
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.eventSequences.EventSequenceId;
import io.cratis.chronicle.eventSequences.EventSequenceNumber;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.geospatial.Point;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.BlockingReactorMethodArgumentResolver;
import io.cratis.chronicle.java.BlockingReactorMiddleware;
import io.cratis.chronicle.observation.EventSequence;
import io.cratis.chronicle.observation.FilterEventsByTag;
import io.cratis.chronicle.observation.ICanBeNotifiedAboutReplay;
import io.cratis.chronicle.observation.OnceOnly;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.observation.Replay;
import io.cratis.chronicle.observation.ReplayContext;
import io.cratis.chronicle.observation.Tag;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetFromContext;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.IReadModelReactor;
import io.cratis.chronicle.readModels.Passive;
import io.cratis.chronicle.readModels.ReadModel;
import io.cratis.chronicle.readModels.ReadModelChangeset;
import io.cratis.chronicle.seeding.ICanSeedEvents;
import io.cratis.chronicle.seeding.IEventSeedingBuilder;
import io.cratis.chronicle.webhooks.IWebhookDefiner;
import io.cratis.chronicle.webhooks.IWebhookDefinitionBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kotlin.reflect.KParameter;

/**
 * Every public entry point of the client, written out in Java.
 *
 * Java is a first-class target, but Kotlin compiling and the specs passing prove nothing about it:
 * default arguments do not exist for Java, {@code @Repeatable} needs an explicit container,
 * annotation elements only get array shorthand when named {@code value}, {@code suspend} functions
 * are unusable, and {@code @JvmInline} mangles its signatures. Every one of those has broken Java in
 * this repository at least once.
 *
 * This file is the guard. It is never run - compiling it <em>is</em> the assertion. Anything that
 * stops being expressible from Java fails the build here, in one place, rather than in someone's
 * application. Add to it whenever a public type or annotation is added.
 *
 * <p>The narrower fixtures elsewhere under {@code src/test/java} cover the same ground per area and
 * are exercised from Kotlin specs; this one is about breadth.
 */
public final class JavaConformance {

    private JavaConformance() {
    }

    // --- Concepts -----------------------------------------------------------------------------

    /** A domain value with a type of its own, declared the way Java declares one. */
    public record EmployeeId(String value) implements ConceptAs<String> {
        @Override
        public String getValue() {
            return value;
        }
    }

    // --- Events -------------------------------------------------------------------------------

    /** An event type, with a concept, a PII property and a geospatial value. */
    @EventType
    public record EmployeeHired(
            EmployeeId employee,
            @Pii String nationalIdentifier,
            String title,
            Point hiredAt) {
    }

    /** A second generation of an event type. */
    @EventType(id = "EmployeeHired", generation = 2)
    public record EmployeeHiredV2(EmployeeId employee, String title) {
    }

    // --- Read models --------------------------------------------------------------------------

    /** A read model. */
    @ReadModel
    public record EmployeeState(String id, String title) {
    }

    // --- Compliance -----------------------------------------------------------------------------

    /** A concept implementing {@link EventSourceId}, the way a Java caller opts a concept into being one. */
    public record CustomerId(String value) implements EventSourceId {
        @Override
        public String getValue() {
            return value;
        }
    }

    /** A concept used purely to prove {@link Pii} resolves from a Java record's canonical constructor parameter. */
    public record NationalIdentifier(String value) implements ConceptAs<String> {
        @Override
        public String getValue() {
            return value;
        }
    }

    /** A record whose {@link ConceptAs} component is annotated {@link Pii} directly - the constructor-parameter shape. */
    public record CustomerRegisteredWithPiiConcept(CustomerId customerId, @Pii NationalIdentifier nationalId) {
    }

    // --- Model-bound projections ----------------------------------------------------------------

    /** An event a model-bound projection sets a constant value from. */
    @EventType
    public record JavaOrderPlaced(String customerName) {
    }

    /** A second event, so {@link SetValue} can be shown repeated across events on the same property. */
    @EventType
    public record JavaOrderCancelled() {
    }

    /**
     * A passive model-bound read model exercising {@link SetValue} (set, repeated, and clear) and
     * {@link SetFromContext}, the way a Java caller declares them - as element arrays via repeated
     * annotations, and named elements rather than Kotlin's positional/default-argument syntax.
     */
    @ReadModel
    @FromEvent(eventType = JavaOrderPlaced.class)
    @FromEvent(eventType = JavaOrderCancelled.class)
    @Passive
    public record JavaOrderStatus(
            @SetFrom(propertyPath = "customerName", eventType = JavaOrderPlaced.class)
            String customerName,

            @SetValue(eventType = JavaOrderPlaced.class, value = "active")
            @SetValue(eventType = JavaOrderCancelled.class, value = "cancelled")
            String status,

            @SetFromContext(eventType = JavaOrderPlaced.class, contextProperty = "occurred")
            String placedAt,

            @SetFrom(propertyPath = "customerName", eventType = JavaOrderPlaced.class)
            @SetValue(eventType = JavaOrderCancelled.class, clear = true)
            String note) {
    }

    // --- Observers ----------------------------------------------------------------------------

    /** A reactor covering every shape a handler can take, plus the replay notifications. */
    @Reactor(id = "java-conformance-alerts")
    @Tag({"hr"})
    @Tag({"onboarding"})
    @FilterEventsByTag("hr")
    @EventSequence("audit")
    public static class Alerts implements ICanBeNotifiedAboutReplay {

        /** (event). */
        public void hired(EmployeeHired event) {
        }

        /** (event, context). */
        @Replay
        public void hiredDuringReplay(EmployeeHired event, EventContext context) {
        }

        /** (event, readModel) - the read model resolved for the event source. */
        public void promoted(EmployeeHiredV2 event, EmployeeState state) {
        }

        /** A handler kept out of replays. */
        @OnceOnly
        public void welcomed(EmployeeHiredV2 event) {
        }

        /** Told when a replay begins. */
        public void replayBegan(ReplayContext context) {
        }

        /** Told when it ends, without asking for the context. */
        public void replayEnded() {
        }
    }

    /** A reducer covering every shape a handler can take. */
    @Reducer(id = "java-conformance-state")
    public static class State {
        /** (event). */
        public EmployeeState hired(EmployeeHired event) {
            return new EmployeeState("", event.title());
        }

        /** (event, state). */
        public EmployeeState promoted(EmployeeHiredV2 event, EmployeeState current) {
            return new EmployeeState(current.id(), event.title());
        }
    }

    /** A read model reactor. */
    public static class Profiles implements IReadModelReactor {
        /** (readModel). */
        public void added(EmployeeState state) {
        }

        /** (readModel, changeset) - a removal never carries an instance. */
        public void removed(EmployeeState state, ReadModelChangeset<EmployeeState> changeset) {
        }
    }

    // --- Cross-cutting ------------------------------------------------------------------------

    /** A reactor middleware, which Java writes without {@code suspend}. */
    public static class Timing implements BlockingReactorMiddleware {
        @Override
        public void beforeInvoke(EventContext context, Object event) {
        }

        @Override
        public void afterInvoke(EventContext context, Object event) {
        }
    }

    /** An argument resolver, likewise. */
    public static class Clocks implements BlockingReactorMethodArgumentResolver {
        @Override
        public boolean canResolve(KParameter parameter) {
            return false;
        }

        @Override
        public Object resolve(KParameter parameter, EventContext context) {
            return null;
        }
    }

    // --- Rules and sources --------------------------------------------------------------------

    /** A constraint. */
    @Constraint
    public static class UniqueNationalIdentifier implements IConstraint {
        @Override
        public void define(IConstraintBuilder builder) {
            builder.uniqueFor(EmployeeHired.class, "One employee per national identifier.");
        }
    }

    /** A seeder. */
    public static class Seed implements ICanSeedEvents {
        @Override
        public void seed(IEventSeedingBuilder builder) {
            builder.forEventSource(
                "employee-1",
                List.of(new EmployeeHired(new EmployeeId("employee-1"), "id", "Engineer", new Point(10.75, 59.91))));
        }
    }

    /** A webhook. */
    public static class Outbound implements IWebhookDefiner {
        @Override
        public void define(IWebhookDefinitionBuilder builder) {
            builder.withEventType(EmployeeHired.class);
        }
    }

    // --- Values -------------------------------------------------------------------------------

    /** Every constructor form of {@link AppendOptions} a Java caller might reach for. */
    public static List<AppendOptions> appendOptions() {
        return List.of(
            new AppendOptions(),
            new AppendOptions(UUID.randomUUID()),
            new AppendOptions(UUID.randomUUID(), ConcurrencyScope.Companion.getNone()),
            new AppendOptionsBuilder()
                .correlationId(UUID.randomUUID())
                .concurrencyScope(ConcurrencyScope.Companion.getNone())
                .eventSourceType("Employee")
                .eventStreamType("Onboarding")
                .eventStreamId("stream-1")
                .subject("employee-1")
                .tag("gdpr")
                .tags(List.of("hr"))
                .occurred(Instant.EPOCH)
                .causation(Causation.of(Instant.EPOCH, "Import"))
                .build());
    }

    /** Every constructor form of {@link EventForEventSourceId}. */
    public static List<EventForEventSourceId> eventsForEventSource() {
        var event = new EmployeeHired(new EmployeeId("employee-1"), "id", "Engineer", new Point(10.75, 59.91));
        return List.of(
            new EventForEventSourceId("employee-1", event),
            new EventForEventSourceId("employee-1", event, "Onboarding"),
            new EventForEventSourceId("employee-1", event, "Onboarding", "stream-1"),
            new EventForEventSourceId(
                "employee-1", event, "Onboarding", "stream-1", "Employee",
                List.of("hr"), Instant.EPOCH, "employee-1",
                List.of(Causation.of(Instant.EPOCH, "Import", Map.of("file", "1998.csv")))));
    }

    /**
     * The value types a Java caller constructs directly.
     *
     * {@code EventSequenceId}, {@code EventSequenceNumber} and {@code CausationType} are deliberately
     * absent. They are {@code @JvmInline value class}es, whose constructors and accessors carry
     * mangled JVM signatures that Java cannot name - so the Java surface never asks for one. Every
     * bridge takes the underlying {@code String} or {@code long} instead, and reading one back works
     * because {@code getValue()} is the one accessor Kotlin leaves unmangled.
     */
    public static Object[] values() {
        return new Object[] {
            new Point(10.75, 59.91),
            new EmployeeId("employee-1"),
            Causation.of(Instant.EPOCH, "Import")
        };
    }

    /** Reading a value class the client handed back, which is the only direction Java has. */
    public static String sequenceIdValue(EventSequenceId id) {
        return id.getValue();
    }

    /** The same for a sequence number. */
    public static long sequenceNumberValue(EventSequenceNumber number) {
        return number.getValue();
    }

    /** Every factory and adjustment on {@link ChronicleOptions}. */
    public static List<ChronicleOptions> options() {
        var development = ChronicleOptions.Companion.development();
        return List.of(
            development,
            ChronicleOptions.Companion.fromConnectionString("chronicle://localhost:35000"),
            development.withoutAutoRegistration(),
            development.withArtifactsFrom("com.acme.ordering", "com.acme.shipping"));
    }
}
