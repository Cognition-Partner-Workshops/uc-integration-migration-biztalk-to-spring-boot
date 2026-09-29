package com.workshop.integration.maps;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MapRegistry {

    private final Map<String, MapTransform> byName;

    public MapRegistry(List<MapTransform> transforms) {
        this.byName = transforms.stream()
            .collect(Collectors.toUnmodifiableMap(MapTransform::name, Function.identity()));
    }

    public Optional<MapTransform> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public List<String> names() {
        return byName.keySet().stream().sorted().toList();
    }
}
