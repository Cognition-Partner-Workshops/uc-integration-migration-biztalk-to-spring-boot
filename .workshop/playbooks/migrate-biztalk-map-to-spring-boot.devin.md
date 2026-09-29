# Playbook: Migrate a BizTalk map to a verified Spring Boot transform

> **Facilitator / presenter:** this file is the source for a **Devin Playbook**.
> Register it in your Devin organization (Settings → Playbooks → *Create a new
> Playbook*) with the macro `!migrate-biztalk-map` so sessions can invoke it.
> The repo-specific commands (build, parity gate, file paths) live in the
> companion Skill at `.agents/skills/biztalk-to-spring-boot-migration/SKILL.md`,
> which Devin auto-loads when working in this repository.

## Overview

Migrate **one or more** BizTalk Server maps (`.btm`) from the estate in
`uc-integration-migration-biztalk-to-spring-boot` into runnable, **verified**
Spring Boot 3.x transforms in the same repository's `spring-boot-app/`. The
outcome is a PR containing the migrated transform(s), a passing build, and
parity evidence: the migrated code, fed the message BizTalk was tested with,
produces the document BizTalk produced.

## The one principle: BizTalk's recorded output is the source of truth

There is no BizTalk runtime on Linux, so parity is measured against the Test
Map evidence the original developers left in the estate — the recorded input
instance and the `*_output.xml` BizTalk generated. A migration reproduces that
output byte-for-byte in canonical XML form. It does not redesign the message.
If BizTalk's output looks odd (a repeated element, a dropped field, an unusual
grouping), reproduce it and **flag it** in a migration note; fixing the
integration's behavior is a separate decision for the team. This is why
"compiles and the XSLT looks right" is not enough and why every migration is
gated by `tools/parity/parity.py`.

## Required from user

- **Map(s) to migrate** — one or more map ids from `tools/parity/fixtures.json`
  (`python3 tools/parity/parity.py list` shows the verifiable ones), or "all
  paired maps".
- **Namespace** — an isolated branch name so concurrent runs do not collide,
  e.g. `migration/map-person` (outputs land on `migration/<namespace>`).

## Procedure

1. Read the skill at `.agents/skills/biztalk-to-spring-boot-migration/SKILL.md`
   and run `python3 tools/parity/parity.py show <map-id>` to read the recorded
   input and BizTalk output before touching code.
2. Read the map source: the `.btm` (UTF-16 XML — `<Links>` for field lineage,
   `<Functoids>` / `<ScripterCode>` for inline XSLT or C#), the source and
   target `.xsd` it references (target namespace, element order), and any
   external `.xsl` the sample ships.
3. Choose a strategy and record why in the PR:
   - **XSLT carry-over** (`XsltMapTransform` + stylesheet under
     `src/main/resources/maps/`) when the map is links + XSLT functoids.
   - **Java rewrite** (`MapTransform` implemented in Java) when the map depends
     on C# scripting functoids or external assemblies.
4. Implement the transform as a Spring bean named after the map, register its
   runner in `tools/parity/runners.json`, and add a JUnit test that asserts the
   fixture pair with XMLUnit.
5. Build and run the gate:
   - `cd spring-boot-app && ./mvnw -q verify` — must be BUILD SUCCESS.
   - `python3 tools/parity/parity.py run <map-id> -- java -jar spring-boot-app/target/biztalk-migration-0.1.0-SNAPSHOT.jar --map=<MapName> {input}` — must print `PASS`.
6. Close the loop: on `FAIL`, read the diff **against the BizTalk output and the
   map's XSLT** — never edit the fixture, the recorded output, or the harness to
   make it pass. Correct the transform and re-run until `PASS`.
7. Deliver a PR from `migration/<namespace>` to `main` containing the
   transform(s), test(s), runner registration, and a note listing any
   BizTalk-faithful quirks reproduced. Do not merge it.

## Specifications (postconditions)

- `./mvnw -q verify` in `spring-boot-app/` is green.
- `parity.py run <map-id>` prints `PASS` for every migrated map, and
  `parity.py run --all` is green for everything registered in `runners.json`.
- Nothing under `Working-with-*/`, `tools/parity/fixtures.json`, or any
  `*_output.xml` is modified.
- Every BizTalk-specific behavior reproduced is flagged in a code comment and
  in the PR description — never silently changed.
- The PR targets `main` from `migration/<namespace>`; `main` is untouched.

## Worked example: a real divergence the gate catches

`MapPerson` (Grouping Pattern — Selecting Distinct Nodes, Sample 3) groups
`Person` records by `Name`. A natural reading of the map produces one
`Nationality` per person. BizTalk's recorded output for `Person1` contains
**two** `Nationality` elements and only the **last** `Email`, because the
map's `NationalityTemplate` emits every match while `EmailTemplate` keeps
`position()=last()`. The gate fails with a two-line diff:

```
FAIL working-with-maps/grouping-pattern-selecting-distinct-nodes/Sample3.MapPerson
  -    <Nationality>Portuguese</Nationality>
  -    <Nationality>English</Nationality>
  +    <Nationality>Portuguese</Nationality>
```

The fix is to carry BizTalk's XSLT semantics over exactly (iterate all matches
for nationality, keep the last email), add a `// BizTalk parity:` note, and
re-run to `PASS`. The point: a reviewer reading the Java would have approved
the "cleaner" output; the recorded BizTalk output caught it.

## Advice and pointers

- Start with `parity.py show` — the recorded output tells you the target
  namespace, element order, and the quirks before you read any XSLT.
- BizTalk samples are UTF-16 with a BOM; the harness and `MapRunner` decode
  them. Never re-encode files in the estate.
- BizTalk omits the XML declaration (`OmitXmlDeclaration="Yes"`); the CLI must
  too. Indentation does not matter — the gate canonicalizes.
- Maps with `status: input-only` or `unresolved` in `fixtures.json` cannot be
  verified. Do not invent an expected output; report them as needing a golden
  from the team.
- If Maven Central rate-limits (`429`), build with
  `./mvnw -s .mvn/settings-gcs-mirror.xml -q verify`.

## Forbidden actions

- Do NOT edit `*_output.xml`, sample inputs, `fixtures.json`, or `parity.py`
  to make a comparison pass.
- Do NOT merge into `main` — it is the durable before-state (estate + harness +
  empty target).
- Do NOT skip the parity gate — a green Maven build is not evidence of parity.
- Do NOT "clean up" BizTalk behavior during migration.
- Do NOT modify anything under `Working-with-*/`.
