# Guides

Task-focused recipes for working with the Chronicle Kotlin client. Each guide assumes you already have a running Chronicle Kernel and a configured `EventStore`. See [Get Started](../get-started/index.md) if you haven't set that up yet.

| Guide | What you'll do |
|---|---|
| [Appending Events](appending-events.md) | Append single events, multiple events, and handle constraint violations |
| [Reactors](reactors.md) | Set up an event observer that runs side effects |
| [Reducers](reducers.md) | Fold events into a mutable read model |
| [Projections](projections.md) | Declare event-to-field mappings without writing fold logic |
| [Constraints](constraints.md) | Enforce uniqueness rules at the event store level |
| [Seeding](seeding.md) | Pre-populate the event log with initial data |
| [PII Compliance](compliance.md) | Annotate sensitive fields and manage their lifecycle |
| [Transactions](transactions.md) | Group multiple appends into an atomic unit of work |
