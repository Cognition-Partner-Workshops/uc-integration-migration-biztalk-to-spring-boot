package com.workshop.integration.maps;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wall clock seen by maps whose BizTalk scripting functoids read {@code DateTime.UtcNow}.
 *
 * <p>The recorded BizTalk outputs freeze whatever the clock said during the original Test Map
 * run, so parity against those goldens is only possible with a pinned instant. The default pins
 * {@code biztalk.maps.clock} to the instant recorded in
 * {@code Enrollment_to_5010_834_output.xml}; set it to {@code system} for live UTC.
 */
@Configuration
public class MapClockConfiguration {

    static final String SYSTEM = "system";

    @Bean
    public Clock mapClock(@Value("${biztalk.maps.clock:2014-06-30T23:11:54.36Z}") String clock) {
        if (SYSTEM.equalsIgnoreCase(clock.trim())) {
            return Clock.systemUTC();
        }
        return Clock.fixed(Instant.parse(clock.trim()), ZoneOffset.UTC);
    }
}
