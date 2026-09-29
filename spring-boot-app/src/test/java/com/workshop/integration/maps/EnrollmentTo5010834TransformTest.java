package com.workshop.integration.maps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

@SpringBootTest
class EnrollmentTo5010834TransformTest {

    private static final Path SAMPLE = Path.of("..", "Working-with-Maps", "Looping-Pattern", "LoopingPattern", "X1EDISample", "Files");
    private static final Path INPUT = SAMPLE.resolve("Enrollment_input.xml");
    private static final Path EXPECTED = SAMPLE.resolve("Enrollment_to_5010_834_output.xml");

    @Autowired
    MapRegistry registry;

    @Test
    void reproducesBizTalkRecordedOutput() throws IOException {
        MapTransform transform = registry.find("Enrollment_to_5010_834").orElseThrow();

        String actual = transform.transform(readXml(INPUT));

        Diff diff = DiffBuilder.compare(readXml(EXPECTED))
            .withTest(actual)
            .ignoreWhitespace()
            .checkForIdentical()
            .build();
        assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
        assertThat(actual).doesNotStartWith("<?xml");
    }

    @Test
    void bgnDateAndTimeComeFromTheClockAndStickLikeBizTalkStatics() throws IOException {
        Instant first = Instant.parse("2026-01-02T03:04:05.678Z");
        MutableClock clock = new MutableClock(first);
        var transform = new EnrollmentTo5010834Transform(clock);
        String input = readXml(INPUT);

        String firstRun = transform.transform(input);
        clock.instant = first.plusSeconds(3600);
        String secondRun = transform.transform(input);

        assertThat(firstRun)
            .contains("<BGN03_TransactionSetCreationDate>20260102</BGN03_TransactionSetCreationDate>")
            .contains("<BGN04_TransactionSetCreationTime>03040567</BGN04_TransactionSetCreationTime>");
        assertThat(secondRun).isEqualTo(firstRun);
    }

    /** BizTalk sample files are UTF-16 or UTF-8 with a BOM. */
    static String readXml(Path path) throws IOException {
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

    private static final class MutableClock extends Clock {
        Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
