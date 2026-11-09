// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs;

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.keys.Key;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import java.util.List;
import kotlin.jvm.JvmClassMappingKt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A property graph written in Java, the way a reader would write one: records and classes as nodes and
 * edges, annotations on their components, and a fluent graph whose callbacks are plain lambdas.
 *
 * A Kotlin-only spec cannot prove any of this compiles for Java, or that Kotlin reflection finds the
 * annotations Java puts on a record component.
 */
public class JavaGraphProjectionUsage {

    /** A person joined the club. */
    @EventType
    public record PersonJoined(String personId, String name) {
    }

    /** A person left the club. */
    @EventType
    public record PersonLeft(String personId) {
    }

    /** Two people became friends. */
    @EventType
    public record BecameFriends(String personId, String friendId, String since) {
    }

    /** A node as a record: the key and the properties are record components. */
    @Graph("JavaClub")
    @Node
    @FromEvent(eventType = PersonJoined.class)
    @RemovedWith(eventType = PersonLeft.class, detach = true)
    public record Member(@Key String personId, String name) {
    }

    /** A node as a JavaBean-shaped class. */
    @Graph("JavaClub")
    @Node
    @FromEvent(eventType = PersonJoined.class)
    public static class Friend {
        @Key
        public String personId = "";
        public String name = "";
    }

    /** An edge as a record, naming the end nodes on the components that hold their keys. */
    @Graph("JavaClub")
    @Edge("FRIEND_OF")
    @EdgeFrom(eventType = BecameFriends.class, sourceKey = "personId", targetKey = "friendId")
    public record FriendOf(
        @EdgeSource(Member.class) String personId,
        @EdgeTarget(Member.class) String friendId,
        String since) {
    }

    /** The same graph declared with the builder. */
    @Graph("JavaClubFluent")
    public static class ClubGraph implements IGraphProjection {
        @Override
        public void define(IGraphProjectionBuilder builder) {
            builder
                .node(FluentMember.class, node -> {
                    node.identifiedBy("personId")
                        .from(PersonJoined.class)
                        .removedWith(PersonLeft.class, removal -> {
                            removal.detach();
                        });
                })
                .edge(FluentFriendOf.class, FluentMember.class, FluentMember.class, edge -> {
                    edge.named("FRIEND_OF")
                        .from(BecameFriends.class, from -> {
                            from.usingSourceKey("personId")
                                .usingTargetKey("friendId")
                                .set("since").toProperty("since")
                                .count("times");
                        });
                });
        }
    }

    /** A fluent node. */
    public static class FluentMember {
        public String personId = "";
        public String name = "";
    }

    /** A fluent edge. */
    public static class FluentFriendOf {
        @EdgeSource(FluentMember.class)
        public String personId = "";
        @EdgeTarget(FluentMember.class)
        public String friendId = "";
        public String since = "";
        public int times;
    }

    @Test
    public void annotatedRecordsAndClassesAreANodesAndEdges() {
        var registration = new GraphProjections("Neo4j")
            .registrationsFor(List.of(
                JvmClassMappingKt.getKotlinClass(Member.class),
                JvmClassMappingKt.getKotlinClass(Friend.class),
                JvmClassMappingKt.getKotlinClass(FriendOf.class))).get(0);
        var graph = registration.getProjection().getGraph();

        assertEquals("JavaClub", registration.getGraphName());
        assertEquals(java.util.Set.of("Member", "Friend"), graph.getNodesMap().keySet());
        assertEquals("$eventSourceId", graph.getNodesMap().get("Member").getFromMap().get("PersonJoined+1").getKey());
        assertEquals("name", graph.getNodesMap().get("Member").getFromMap().get("PersonJoined+1").getPropertiesMap().get("name"));
        assertTrue(graph.getNodesMap().get("Member").getRemovedWithMap().get("PersonLeft+1").getDetach());
        var from = graph.getEdgesMap().get("FRIEND_OF").getFromMap().get("BecameFriends+1");
        assertEquals("personId", from.getSourceKey());
        assertEquals("friendId", from.getTargetKey());
        assertEquals("since", from.getPropertiesMap().get("since"));
        assertEquals(java.util.Set.of("since"), java.util.Set.copyOf(from.getPropertiesMap().keySet()));
    }

    @Test
    public void aFluentGraphCanBeWrittenWithLambdas() {
        var registration = new GraphProjections("InMemoryGraph").registrationsFor(List.of(new ClubGraph())).get(0);
        ProjectionsOuterClass.GraphProjectionDefinition graph = registration.getProjection().getGraph();

        assertEquals("JavaClubFluent", registration.getGraphName());
        assertEquals("InMemoryGraph", registration.getReadModel().getSink().getTypeId());
        assertEquals("$count", graph.getEdgesMap().get("FRIEND_OF").getFromMap().get("BecameFriends+1").getPropertiesMap().get("times"));
        assertTrue(graph.getNodesMap().get("FluentMember").getRemovedWithMap().get("PersonLeft+1").getDetach());
    }
}
