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
 * {@code working-with-maps/muenchian-grouping-and-sorting-without-losing-map-functionalities/Sample1.MapOrderUsingCount}.
 */
class MapOrderUsingCountTransformTest {

    private static final Path SAMPLE = Path.of("..",
        "Working-with-Maps", "Muenchian-Grouping-and-Sorting-without-losing-Map-functionalities", "Samples", "1");

    private final MapOrderUsingCountTransform transform = new MapOrderUsingCountTransform();

    @Test
    void nameIsTheBizTalkMapName() {
        assertThat(transform.name()).isEqualTo("MapOrderUsingCount");
    }

    @Test
    void reproducesBizTalkRecordedOutput() throws IOException {
        String input = readXml(SAMPLE.resolve("InputOrder.xml"));
        String expected = readXml(SAMPLE.resolve("InputOrder.xml_output.xml"));

        String actual = transform.transform(input);

        assertThat(actual).doesNotStartWith("<?xml");
        Diff diff = DiffBuilder.compare(expected).withTest(actual)
            .ignoreWhitespace()
            .checkForSimilar()
            .build();
        assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
    }

    /** BizTalk sample messages are UTF-16 with a BOM. */
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
