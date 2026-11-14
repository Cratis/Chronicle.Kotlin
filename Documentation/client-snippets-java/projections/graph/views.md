```java title="A graph view that reads courses through an edge"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.keys.Key;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFromContext;
import io.cratis.chronicle.projections.graphs.Edge;
import io.cratis.chronicle.projections.graphs.EdgeFrom;
import io.cratis.chronicle.projections.graphs.EdgeSource;
import io.cratis.chronicle.projections.graphs.EdgeTarget;
import io.cratis.chronicle.projections.graphs.Follows;
import io.cratis.chronicle.projections.graphs.FromEdge;
import io.cratis.chronicle.projections.graphs.Graph;
import io.cratis.chronicle.projections.graphs.GraphViews;
import io.cratis.chronicle.projections.graphs.Node;
import io.cratis.chronicle.projections.graphs.ViewOf;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@EventType
record StudentRegistered(UUID studentId, String name) {}

@EventType
record CourseOpened(UUID courseId, String title) {}

@EventType
record StudentEnrolled(UUID studentId, UUID courseId) {}

@Graph("Campus")
@Node
@FromEvent(eventType = StudentRegistered.class)
record Student(@Key UUID studentId, String name) {}

@Graph("Campus")
@Node
@FromEvent(eventType = CourseOpened.class)
record Course(@Key UUID courseId, String title) {}

@Graph("Campus")
@Edge("ENROLLED_IN")
@EdgeFrom(eventType = StudentEnrolled.class, sourceKey = "studentId", targetKey = "courseId")
record EnrolledIn(
    @EdgeSource(Student.class) UUID studentId,
    @EdgeTarget(Course.class) UUID courseId,
    @SetFromContext(eventType = StudentEnrolled.class, contextProperty = "occurred") Instant since) {}

// The view of one course, with the property of the edge that was followed to reach it.
@ViewOf(Course.class)
record CourseEnrollment(@Key UUID id, String title, @FromEdge Instant since) {}

// The view of one student: the key, a property and the courses along ENROLLED_IN.
@ViewOf(Student.class)
record StudentWithCourses(
    @Key UUID id,
    String name,
    @Follows(EnrolledIn.class) List<CourseEnrollment> courses) {}

// Check the views and get the definition of each graph that has them. Nothing here is sent to the kernel.
class CourseViewDefinitions {
    static String definition() {
        return GraphViews.definitionsFor(
            Student.class, Course.class, EnrolledIn.class, CourseEnrollment.class, StudentWithCourses.class)
            .get(0)
            .toJson();
    }
}
```
