```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.artifacts.IArtifactActivator
import io.cratis.chronicle.artifacts.IClientArtifacts
import io.cratis.chronicle.connection.ChronicleConnectionString

/**
 * Unlike the .NET client, structural dependencies are not separate constructor parameters on
 * [ChronicleClient] — they are carried on [ChronicleOptions] itself, which is where every named
 * dependency below actually lives.
 */
fun createClient(artifacts: IClientArtifacts, artifactActivator: IArtifactActivator): ChronicleClient {
    val options = ChronicleOptions(
        connectionString = ChronicleConnectionString.DEVELOPMENT,
        artifacts = artifacts,
        artifactActivator = artifactActivator
    )
    return ChronicleClient(options)
}
```
