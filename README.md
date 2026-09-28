# BizTalk Server → Spring Boot Integration Migration

Source estate for migrating Microsoft BizTalk Server integrations to Java / Spring Boot 3.x
(Spring Integration or Apache Camel on Spring Boot).

The BizTalk artifacts in this repo are imported, with full git history, from
[sandroasp/BizTalk-Server-Learning-Path](https://github.com/sandroasp/BizTalk-Server-Learning-Path)
(MIT, © Sandro Pereira). Each folder is a self-contained BizTalk Visual Studio solution
that demonstrates one integration pattern, which makes the estate a good fit for splitting the
migration into independent, parallel work items.

## Estate inventory

| Area | Samples | Orchestrations (`.odx`) | Maps (`.btm`) | Schemas (`.xsd`) | Pipelines (`.btp`) | BizTalk projects (`.btproj`) |
|------|--------:|------:|------:|------:|------:|------:|
| `Working-with-Maps/` | 34 | 9 | 151 | 152 | 0 | 35 |
| `Working-with-Orquestrations/` | 11 | 21 | 3 | 26 | 0 | 13 |
| `Working-with-Schemas/` | 8 | 0 | 1 | 15 | 8 | 7 |
| `Working-with-Routing/` | 1 | 1 | 6 | 7 | 1 | 1 |
| `Working-with-Pipelines/` | 1 | 0 | 0 | 0 | 1 | 1 |
| `Working-with-Adapters/` | 2 | 2 | 0 | 3 | 0 | 2 |
| `Testing-Deploying-and-Managing-BizTalk-Applications/` | 2 | 0 | 8 | 8 | 0 | 2 |
| `Tracking-and-Troubleshooting-BizTalk-Applications/` | 1 | 1 | 0 | 0 | 0 | 1 |

Artifacts also include binding files (`*BindingInfo.xml`), WCF-SQL adapter schemas, custom
functoids and pipeline components (C#), BizTalk unit tests, and sample input/output messages.

## BizTalk → Spring Boot concept mapping

| BizTalk artifact | Typical Spring Boot target |
|------------------|----------------------------|
| Schema (`.xsd`), flat-file schema | JAXB / Jackson models; Spring Batch `FlatFileItemReader` or BeanIO for delimited/positional files |
| Map (`.btm`) + functoids / inline C# / XSLT | XSLT via `javax.xml.transform`, or Java mapping code (MapStruct) with unit tests over sample messages |
| Orchestration (`.odx`) | Spring Integration flow / Camel route, or a `@Service` with explicit state and error handling |
| Receive / send pipelines, pipeline components | Spring Integration transformers/filters, Camel processors |
| Receive locations / send ports, adapters (File, WCF-SQL, Service Bus) | Spring Integration / Camel endpoints (file, JDBC, Azure Service Bus / JMS) |
| Content-based routing, promoted properties, filters | Message headers + routers (`@Router`, Camel `choice()`) |
| Correlation sets, direct-bound ports | Aggregators, correlation strategies, channels |
| Exception handling scopes | Error channels, retry/`@Retryable`, dead-letter handling |
| Binding files | `application.yml` configuration and profiles |

## Suggested migration approach

1. Inventory the estate: for each sample, catalog its schemas, maps, orchestrations, pipelines, and ports.
2. Pick a pattern (for example `Working-with-Maps/Content-Enricher-Pattern`) and produce a Spring Boot module that reproduces the same input → output behavior.
3. Prove parity with tests that replay the sample input messages in the sample folder and compare the output with the BizTalk-generated output.
4. Fan out the remaining samples as independent work items. Each sample is a separate solution, so migrations can run in parallel.

## Layout notes

- Folder names are kept as they are upstream (including `Working-with-Orquestrations`) so history and links still resolve.
- Some samples include committed `bin/` and `obj/` build output from the upstream repo. Treat these as reference only.
- Some samples include Portuguese-language material (for example `ComoFuncinamOsMapas`), as in the upstream repo.

## License

MIT. See [`LICENSE`](LICENSE). Original work © Sandro Pereira.
