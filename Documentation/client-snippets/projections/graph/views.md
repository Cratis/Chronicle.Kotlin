```kotlin title="A graph view that reads courses through an edge"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.graphs.Edge
import io.cratis.chronicle.projections.graphs.EdgeFrom
import io.cratis.chronicle.projections.graphs.EdgeSource
import io.cratis.chronicle.projections.graphs.EdgeTarget
import io.cratis.chronicle.projections.graphs.Follows
import io.cratis.chronicle.projections.graphs.FromEdge
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.GraphViews
import io.cratis.chronicle.projections.graphs.Node
import io.cratis.chronicle.projections.graphs.ViewOf
import java.time.Instant
import java.util.UUID

@EventType
data class StudentRegistered(val studentId: UUID, val name: String)

@EventType
data class CourseOpened(val courseId: UUID, val title: String)

@EventType
data class StudentEnrolled(val studentId: UUID, val courseId: UUID)

@Graph("Campus")
@Node
@FromEvent(StudentRegistered::class)
data class Student(@Key val studentId: UUID, val name: String)

@Graph("Campus")
@Node
@FromEvent(CourseOpened::class)
data class Course(@Key val courseId: UUID, val title: String)

@Graph("Campus")
@Edge("ENROLLED_IN")
@EdgeFrom(StudentEnrolled::class, sourceKey = "studentId", targetKey = "courseId")
data class EnrolledIn(
    @EdgeSource(Student::class) val studentId: UUID,
    @EdgeTarget(Course::class) val courseId: UUID,
    @SetFromContext(StudentEnrolled::class, "occurred") val since: Instant
)

// The view of one course, with the property of the edge that was followed to reach it.
@ViewOf(Course::class)
data class CourseEnrollment(@Key val id: UUID, val title: String, @FromEdge val since: Instant)

// The view of one student: the key, a property and the courses along ENROLLED_IN.
@ViewOf(Student::class)
data class StudentWithCourses(
    @Key val id: UUID,
    val name: String,
    @Follows(EnrolledIn::class) val courses: List<CourseEnrollment>
)

// Check the views and get the definition of each graph that has them. Nothing here is sent to the kernel.
fun courseViewDefinitions(): String =
    GraphViews.definitionsFor(
        Student::class, Course::class, EnrolledIn::class, CourseEnrollment::class, StudentWithCourses::class
    ).single().toJson()
```
