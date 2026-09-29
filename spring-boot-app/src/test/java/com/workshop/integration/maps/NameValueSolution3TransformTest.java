package com.workshop.integration.maps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/** Asserts the recorded BizTalk Test Map fixture pair from tools/parity/fixtures.json. */
class NameValueSolution3TransformTest {

    private static final Path SAMPLE = Path.of("..", "Working-with-Maps",
        "Name-Value-Transformation-Pattern-Hierarchical-Schema-to-Name-Value-Pair", "Files");

    private final NameValueSolution3Transform transform = new NameValueSolution3Transform();

    @Test
    void nameMatchesBizTalkMap() {
        assertThat(transform.name()).isEqualTo("NameValueSolution3");
    }

    @Test
    void reproducesRecordedBizTalkOutput() throws IOException {
        String actual = transform.transform(readXml(SAMPLE.resolve("Request.xml")));

        assertThat(actual).doesNotStartWith("<?xml");
        Diff diff = DiffBuilder.compare(readXml(SAMPLE.resolve("NameValueSolution3_output.xml")))
            .withTest(actual)
            .ignoreWhitespace()
            .checkForIdentical()
            .build();
        assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
    }

    @Test
    void skipsEmptyValuesButAlwaysEmitsProperties() {
        String actual = transform.transform("""
            <ns0:Request xmlns:ns0="http://SandroPereira.MappingToNameValueRecord.Request">
              <Header><Nprocesso>P1</Nprocesso><Tipo_Operacao>X</Tipo_Operacao></Header>
              <Body><ServiceName>S</ServiceName><Type></Type><LAN><IPRoute/></LAN></Body>
            </ns0:Request>""");

        Diff diff = DiffBuilder.compare("""
            <ns0:Provisioning xmlns:ns0="http://SandroPereira.MappingToNameValueRecord.Provisioning">
              <Id>P1</Id><ns0:Properties/><ServiceName>S</ServiceName>
            </ns0:Provisioning>""")
            .withTest(actual)
            .ignoreWhitespace()
            .checkForIdentical()
            .build();
        assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
    }

    /** BizTalk samples are UTF-16 (input) or UTF-8 (output) with a BOM. */
    private static String readXml(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xfe) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
