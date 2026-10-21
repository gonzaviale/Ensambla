package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Los kits por rubro de {@code ia/kits-rubro.json}, en el orden en que se ofrecen. */
public record KitsRubro(List<KitRubro> kits) {

    public KitsRubro {
        kits = List.copyOf(Objects.requireNonNullElse(kits, List.of()));
    }

    /** Por id o por nombre, sin importar mayúsculas. */
    public Optional<KitRubro> buscar(String rubro) {
        return kits.stream()
                .filter(kit -> kit.id().equalsIgnoreCase(rubro) || kit.nombre().equalsIgnoreCase(rubro))
                .findFirst();
    }
}
