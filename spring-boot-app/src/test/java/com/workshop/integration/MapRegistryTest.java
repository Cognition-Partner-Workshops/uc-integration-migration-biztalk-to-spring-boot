package com.workshop.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.workshop.integration.maps.MapRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MapRegistryTest {

    @Autowired
    MapRegistry registry;

    @Test
    void registryHasNoDuplicateNames() {
        assertThat(registry.names()).doesNotHaveDuplicates();
    }
}
