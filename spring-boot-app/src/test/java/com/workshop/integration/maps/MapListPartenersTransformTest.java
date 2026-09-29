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
 * Fixture pair from tools/parity/fixtures.json for
 * {@code working-with-maps/grouping-pattern-selecting-distinct-nodes/Sample1.MapListParteners}.
 *
 * <p>The recorded input ({@code Msg/Input.xml}) and the BizTalk-recorded output
 * ({@code Msg/MapListParteners_output.xml}) were not captured from the same message: the
 * input's third employee works for {@code DemoCompany2}, while the recorded output lists
 * {@code Real Madrid}, a string that appears nowhere in the input. The sample README documents
 * {@code DemoCompany2} as the expected output for this input. The tests therefore pin both
 * facts separately: the transform on the recorded input yields the README-documented output,
 * and the transform on the input the recorded output evidently came from yields the recorded
 * output. Neither fixture file is modified.
 */
class MapListPartenersTransformTest {

    private static final Path SAMPLE = Path.of("..", "Working-with-Maps",
        "Grouping-Pattern-Selecting-distinct-nodes", "Msg");

    private final MapListPartenersTransform transform = new MapListPartenersTransform();

    @Test
    void nameMatchesBizTalkMap() {
        assertThat(transform.name()).isEqualTo("MapListParteners");
    }

    @Test
    void recordedInputYieldsReadmeDocumentedDistinctCompanies() throws IOException {
        String actual = transform.transform(readFixture("Input.xml"));

        assertThat(actual).doesNotStartWith("<?xml");
        assertIdentical("""
            <ns0:ListPartners xmlns:ns0="http://SelectDistinctValues.Output1">
              <PartnerName>DevScope</PartnerName>
              <PartnerName>DemoCompany2</PartnerName>
              <PartnerName>DemoCompany</PartnerName>
            </ns0:ListPartners>
            """, actual);
    }

    @Test
    void inputBehindRecordedOutputYieldsRecordedOutput() throws IOException {
        String inputAsRecorded = readFixture("Input.xml").replace("DemoCompany2", "Real Madrid");

        String actual = transform.transform(inputAsRecorded);

        assertIdentical(readFixture("MapListParteners_output.xml"), actual);
    }

    @Test
    void recordedFixturePairIsKnownToDiverge() throws IOException {
        String actual = transform.transform(readFixture("Input.xml"));

        Diff diff = DiffBuilder.compare(readFixture("MapListParteners_output.xml")).withTest(actual)
            .ignoreWhitespace().checkForIdentical().build();
        assertThat(diff.hasDifferences())
            .as("recorded output contains 'Real Madrid', which the recorded input cannot produce")
            .isTrue();
        assertThat(readFixture("Input.xml")).doesNotContain("Real Madrid");
    }

    private static void assertIdentical(String expected, String actual) {
        Diff diff = DiffBuilder.compare(expected).withTest(actual)
            .ignoreWhitespace().checkForIdentical().build();
        assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
    }

    private static String readFixture(String file) throws IOException {
        byte[] bytes = Files.readAllBytes(SAMPLE.resolve(file));
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xfe) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
