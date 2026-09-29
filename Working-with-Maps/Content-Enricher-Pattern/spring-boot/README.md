# Content-Enricher-Pattern — Spring Boot migration

Java port of the `JoinMultipleMessages` BizTalk solution in the parent folder: the
`TransformUserAndAddressToOutput` map (approach 3, XSLT template) that joins a `Users`
message with an `Addresses` message on `UserID` and emits the `Informations` document.

| BizTalk artifact | Spring Boot equivalent |
|---|---|
| `InuputUsers.xsd`, `InputAddresses.xsd`, `OutputMessage.xsd` | `User`/`Address` records, `Namespaces`, DOM output in `UsersAddressesMapper` |
| `TransformUserAndAddressToOutput.btm` (XSLT `AddressTemplate`) | `UsersAddressesMapper.buildInformations` |
| Orchestration receive of two messages + Transform shape | `POST /api/enrich` (multipart `users` + `addresses`) |
| Transform shape aggregate input (`aggschema` `Root`) | `POST /api/enrich/aggregated` |

## Build, test, run

```bash
cd Working-with-Maps/Content-Enricher-Pattern/spring-boot
mvn -B package                        # JDK 17, runs the parity tests
java -jar target/content-enricher-0.1.0-SNAPSHOT.jar
curl -F users=@../TestFiles/InuputUsers.xml -F addresses=@../TestFiles/InputAddresses.xml localhost:8080/api/enrich
curl -H 'Content-Type: application/xml' --data-binary @../TestFiles/inputfilemultiple.xml localhost:8080/api/enrich/aggregated
curl localhost:8080/actuator/health
```

## Parity tests

`src/test/resources/expected/*.xml` were produced by running the map's XSLT (as embedded
in the `.btm`) with `xsltproc` over the UTF-16 sample messages in `../TestFiles`. The tests
replay those samples through the Java mapper and the HTTP endpoints and require identical
XML. `TestFiles/OutputMessage.xml` is also compared; it differs only in one hand-typed text
node upstream (`18 avec Gerard Majax` vs the input's `18 avenue Gerard Majax`).
