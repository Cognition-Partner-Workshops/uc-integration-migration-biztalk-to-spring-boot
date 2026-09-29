package com.workshop.integration.maps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * Asserts the fixture pair recorded in {@code tools/parity/fixtures.json} for
 * {@code working-with-maps/grouping-pattern-selecting-distinct-nodes/Sample3.MapPerson}.
 */
class MapPersonTransformTest {

    private static final Path ESTATE = Path.of("..", "Working-with-Maps",
        "Grouping-Pattern-Selecting-distinct-nodes", "Msg");

    private final MapPersonTransform transform = new MapPersonTransform();

    @Test
    void nameIsTheBizTalkMapName() {
        assertThat(transform.name()).isEqualTo("MapPerson");
    }

    @Test
    void reproducesRecordedBizTalkOutput() throws IOException {
        String input = readXml(ESTATE.resolve("InputPersons.xml"));
        String expected = readXml(ESTATE.resolve("MapPerson_output.xml"));

        String actual = transform.transform(input);

        assertThat(actual).doesNotStartWith("<?xml");
        Diff diff = DiffBuilder.compare(expected).withTest(actual)
            .ignoreWhitespace()
            .checkForIdentical()
            .build();
        assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
    }

    @Test
    void groupedPersonKeepsAllNationalitiesAndLastEmail() {
        // BizTalk parity: NationalityTemplate emits every match, EmailTemplate keeps the last.
        String input = """
            <ns0:Persons xmlns:ns0="http://SelectDistinctValues.InputPersons">
              <Person><Name>A</Name><Nationality>N1</Nationality><Email>e1</Email></Person>
              <Person><Name>A</Name><Nationality>N2</Nationality><Email>e2</Email></Person>
              <Person><Name>A</Name><Nationality>N3</Nationality><Email>e3</Email></Person>
            </ns0:Persons>
            """;
        String expected = """
            <ns0:Persons xmlns:ns0="http://SelectDistinctValues.OutputPersons">
              <Person>
                <Name>A</Name>
                <Nationality>N1</Nationality>
                <Nationality>N2</Nationality>
                <Nationality>N3</Nationality>
                <Email>e3</Email>
              </Person>
            </ns0:Persons>
            """;

        Diff diff = DiffBuilder.compare(expected).withTest(transform.transform(input))
            .ignoreWhitespace()
            .checkForIdentical()
            .build();
        assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
    }

    /** Estate samples are UTF-16 LE with a BOM; decode like {@code MapRunner} does. */
    private static String readXml(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xfe) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xff) == 0xff) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
