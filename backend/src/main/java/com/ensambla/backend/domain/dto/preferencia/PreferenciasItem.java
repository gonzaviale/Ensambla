package com.ensambla.backend.domain.dto.preferencia;

import java.util.List;
import java.util.Map;

public record PreferenciasItem(
        List<String> descartadas,
        Map<String, Boolean> grupos,
        boolean bienvenidaVista,
        String nombre) {
}
