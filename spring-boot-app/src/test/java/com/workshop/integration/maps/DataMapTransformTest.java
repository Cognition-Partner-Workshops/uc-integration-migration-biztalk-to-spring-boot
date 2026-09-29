package com.workshop.integration.maps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

class DataMapTransformTest {

    private static final Path SAMPLE_DIR = Path.of(
        "..", "Working-with-Maps", "Grouping-Pattern-Selecting-distinct-nodes", "Msg");

    @Test
    void reproducesBizTalkRecordedOutput() throws IOException {
        var transform = new DataMapTransform();
        String actual = transform.transform(read(SAMPLE_DIR.resolve("DataInput.xml")));
        String expected = read(SAMPLE_DIR.resolve("DataMap_output.xml"));

        assertThat(transform.name()).isEqualTo("DataMap");
        assertThat(actual).doesNotStartWith("<?xml");
        Diff diff = DiffBuilder.compare(expected).withTest(actual)
            .ignoreWhitespace()
            .checkForIdentical()
            .build();
        assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
    }

    /** BizTalk samples are UTF-16/UTF-8 with a BOM; strip it the same way MapRunner does. */
    private static String read(Path path) throws IOException {
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
