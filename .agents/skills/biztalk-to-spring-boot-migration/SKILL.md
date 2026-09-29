---
name: biztalk-to-spring-boot-migration
description: >
  Repo-specific mechanics for migrating BizTalk Server maps to Spring Boot 3.x
  in this repository: where the source of truth is, how to build the target,
  how to run the parity gate, and which BizTalk constructs map to which Java
  patterns. Auto-loaded when Devin works in this repository.
---

# BizTalk → Spring Boot Migration Skill

## Repository layout

```
uc-integration-migration-biztalk-to-spring-boot/
├── Working-with-Maps/ …              ← BizTalk estate (read-only reference)
├── Working-with-Orquestrations/ …       .btm maps, .xsd schemas, .odx orchestrations,
├── Working-with-Schemas/ …              .btp pipelines, C# functoids, sample messages
├── tools/parity/
│   ├── inventory.py                  ← builds fixtures.json from the estate
│   ├── fixtures.json                 ← map → recorded input → BizTalk output
│   ├── parity.py                     ← the gate: canonical-XML diff vs BizTalk output
│   └── runners.json                  ← map-id → command that produces the output
└── spring-boot-app/                  ← TARGET: Devin writes migrated maps here
    ├── pom.xml                       ← Spring Boot 3.5, Java 17, Saxon-HE, XMLUnit
    └── src/main/java/com/workshop/integration/
        ├── maps/MapTransform.java    ← interface: name() + transform(xml)
        ├── maps/XsltMapTransform.java← base class for XSLT-carried maps
        ├── maps/MapRegistry.java     ← finds transforms by BizTalk map name
        └── cli/MapRunner.java        ← `--map=<Name> <input.xml>` → stdout
```

## The source of truth

There is no BizTalk runtime on Linux (BizTalk needs Windows + the BizTalk SDK),
so **the recorded Test Map evidence is the oracle**:

- `*.btproj.user` → `<TestMapInputInstanceFilename>` names the input a map was
  tested with (Windows path; only the file name matters).
- `*_output.xml` next to the samples is the output **BizTalk itself produced**.

`tools/parity/inventory.py` joins those into `tools/parity/fixtures.json`. Maps
with `status: paired` have both an input and a recorded output and are the
parity gates. Everything else (`input-only`, `unresolved`) is listed so the gap
is visible; a human has to supply a golden output before those can be verified.

Never edit a `*_output.xml`, an input sample, or `fixtures.json` by hand to make
a comparison pass. If BizTalk's output looks wrong, reproduce it and flag it.

## Commands

```bash
# Rebuild the fixture inventory (idempotent)
python3 tools/parity/inventory.py

# What can be verified, and what its fixtures are
python3 tools/parity/parity.py list
python3 tools/parity/parity.py show <map-id>

# Build the target (Java 17)
cd spring-boot-app && ./mvnw -q verify      # or: mvn -q verify

# Run one map through the gate
python3 tools/parity/parity.py run <map-id> -- \
  java -jar spring-boot-app/target/biztalk-migration-0.1.0-SNAPSHOT.jar --map=<MapName> {input}

# Run every map that has a runner registered in tools/parity/runners.json
python3 tools/parity/parity.py run --all
```

`parity.py run` prints `PASS <map-id>` or `FAIL <map-id>` plus a unified diff
of pretty-printed canonical XML (BOM/declaration dropped, UTF-16 decoded,
whitespace-only text removed). Exit 0 = parity. Element order, attributes and
namespaces are significant.

If Maven Central answers `429 Too Many Requests`, use the GCS mirror:
`mvn -s .mvn/settings-gcs-mirror.xml -q verify`.

## Migrating one map

1. `parity.py show <map-id>` — read the recorded input and BizTalk output first.
2. Read the `.btm` (UTF-16 XML): `<SrcTree>`/`<TrgTree>` reference the XSDs,
   `<Links>` are the field lineage, `<Functoids>` carry inline XSLT / C#
   (`<ScripterCode>`). Read the referenced XSDs for the target namespace and
   element order.
3. Implement a `MapTransform` bean in `spring-boot-app/src/main/java/com/workshop/integration/maps/`
   named after the map (`MapPersonTransform`, `name()` returns `MapPerson`).
   Two acceptable strategies:
   - **XSLT carry-over** — extract/reconstruct the map's XSLT into
     `src/main/resources/maps/<MapName>.xslt` and extend `XsltMapTransform`.
     Fastest, lowest risk, keeps BizTalk semantics (including quirks).
   - **Java rewrite** — DOM/JAXB code that produces the same document. Use when
     the map has C# scripting functoids or when the team wants readable Java.
4. Register the runner in `tools/parity/runners.json`:
   ```json
   { "<map-id>": ["java", "-jar", "spring-boot-app/target/biztalk-migration-0.1.0-SNAPSHOT.jar", "--map=<MapName>", "{input}"] }
   ```
5. Add a JUnit test in `spring-boot-app/src/test/java/...` that loads the same
   fixture pair and asserts with XMLUnit, so `mvn verify` alone proves parity.
6. `mvn -q verify` then `parity.py run <map-id> -- …` until `PASS`.
7. Commit on a `migration/<namespace>` branch; open a PR against `main`. Do
   not merge — `main` is the durable before-state.

## BizTalk → Spring Boot mapping reference

| BizTalk construct | Spring Boot equivalent |
|---|---|
| `.btm` map (compiles to XSLT 1.0) | `XsltMapTransform` (Saxon) or Java `MapTransform` |
| Functoid links / string, math, logical functoids | XSLT templates or Java helper methods |
| Scripting functoid (inline C#) | Java method in the transform; port logic, keep behavior |
| Scripting functoid (inline XSLT / call-template) | Same XSLT under `src/main/resources/maps/` |
| Muenchian grouping / `generate-id()` in maps | XSLT carry-over, or `Collectors.groupingBy` preserving first-seen order |
| `.xsd` schema (target namespace, element order) | Same XSD; JAXB classes only if a Java rewrite needs them |
| Flat-file schema annotations | Spring Batch `FlatFileItemReader` / custom parser (out of scope for maps) |
| `.btp` receive/send pipeline | Spring Integration channel chain / Camel route |
| `.odx` orchestration | Spring Integration flow; durable state needs a workflow engine — human decision |
| Promoted properties / CBR filters | Message headers + Spring Integration router |
| Bindings (`*.BindingInfo.xml`) | `application-<profile>.yml` |

## Quirks you will meet

- Samples are **UTF-16 with BOM**; `MapRunner.readXml` and `parity.py` both
  decode them. Don't convert files in place.
- BizTalk output has **no XML declaration** (`OmitXmlDeclaration="Yes"` in the
  `.btm`) and no indentation. The gate canonicalizes, so indentation is fine,
  but the declaration must be omitted from the CLI output.
- Some recorded outputs contain **BizTalk-faithful oddities** (repeated
  elements, dropped fields) that come from the map's XSLT. The gate expects
  BizTalk's behavior, not your reading of the intent. Reproduce it, add a
  `// BizTalk parity:` note, and call it out in the PR.
- Namespace prefixes in the output are `ns0`; canonical comparison keys on the
  namespace URI, not the prefix.

## Verification checklist before opening a PR

- [ ] `cd spring-boot-app && mvn -q verify` → BUILD SUCCESS
- [ ] `python3 tools/parity/parity.py run <map-id> -- java -jar … --map=<Name> {input}` → `PASS`
- [ ] runner registered in `tools/parity/runners.json`; `parity.py run --all` green
- [ ] no edits under `Working-with-*/`, no edits to `fixtures.json` or `*_output.xml`
- [ ] work is on `migration/<namespace>`, PR targets `main`, nothing merged
