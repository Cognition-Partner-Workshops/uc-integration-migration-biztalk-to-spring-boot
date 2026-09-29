package com.workshop.integration.cli;

import com.workshop.integration.maps.MapRegistry;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Command-line entry point used by the parity harness ({@code tools/parity/parity.py}):
 *
 * <pre>
 *   java -jar target/biztalk-migration-*.jar --map=MapPerson path/to/input.xml
 * </pre>
 *
 * Writes the transformed document to stdout and exits 0; any failure exits non-zero.
 * Without {@code --map} the application starts normally as a web service.
 */
@Component
public class MapRunner implements ApplicationRunner {

    private final MapRegistry registry;
    private final ApplicationContext context;

    public MapRunner(MapRegistry registry, ApplicationContext context) {
        this.registry = registry;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("map")) {
            return;
        }
        String name = args.getOptionValues("map").get(0);
        if (args.getNonOptionArgs().isEmpty()) {
            exit(2, "usage: --map=<MapName> <input.xml>");
            return;
        }
        var transform = registry.find(name).orElse(null);
        if (transform == null) {
            exit(3, "unknown map '" + name + "'; registered: " + registry.names());
            return;
        }
        String input = readXml(Path.of(args.getNonOptionArgs().get(0)));
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.print(transform.transform(input));
        out.flush();
        exit(0, null);
    }

    /** BizTalk sample messages are frequently UTF-16 with a BOM. */
    static String readXml(Path path) throws java.io.IOException {
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

    private void exit(int code, String message) {
        if (message != null) {
            System.err.println(message);
        }
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
