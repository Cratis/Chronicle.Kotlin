# Real-kernel compatibility gate

The fast `test` and `build` tasks do not require Docker. Protocol compatibility
is verified separately against a real all-in-one Chronicle DEVELOPMENT kernel:

```shell
./gradlew :Source:realKernelTest
```

The verified default pair is exact:

- JVM contracts: `io.cratis:chronicle-contracts:16.44.1`
- Kernel image: `cratis/chronicle:16.44.1-development@sha256:3e0216892632f87e5386649cf8c1a189573cf82999abf14b7f6031863a6e545f`

The task maps the kernel's TLS port dynamically, waits for an HTTPS `200` from
`/health` with a two-minute bound, and connects through the normal development
OAuth client `chronicle-dev-client`/`chronicle-dev-secret`. Running this
explicit task without an available Docker daemon is an error; normal unit tests
neither start Docker nor silently skip anything.

For a deliberate compatibility investigation, override only the image with a
Gradle property:

```shell
./gradlew :Source:realKernelTest \
  -PchronicleKernelImage=cratis/chronicle:16.45.0-development@sha256:<verified-oci-index-digest>
```

An override is not the repository's verified pairing until the contracts
dependency and pinned default are deliberately updated together. Do not
substitute a mutable `latest` tag in automation or documentation.

The `Real Kernel Compatibility` GitHub workflow runs this gate directly for
relevant pull requests. The publication workflow invokes the same reusable
workflow and cannot publish Maven artifacts until it succeeds.
