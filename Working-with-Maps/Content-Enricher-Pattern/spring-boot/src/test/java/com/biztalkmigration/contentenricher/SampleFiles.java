package com.biztalkmigration.contentenricher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.w3c.dom.Document;

/** Locates the upstream sample messages in ../TestFiles (Maven runs tests from the module root). */
final class SampleFiles {

    static final Path TEST_FILES = Path.of("..", "TestFiles");

    private SampleFiles() {
    }

    static Document sample(String name) throws IOException {
        try (InputStream in = Files.newInputStream(TEST_FILES.resolve(name))) {
            return XmlSupport.parse(in);
        }
    }

    static String sampleText(String name) throws IOException {
        return XmlSupport.serialize(sample(name));
    }

    static String expected(String name) throws IOException {
        try (InputStream in = SampleFiles.class.getResourceAsStream("/expected/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource expected/" + name);
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
