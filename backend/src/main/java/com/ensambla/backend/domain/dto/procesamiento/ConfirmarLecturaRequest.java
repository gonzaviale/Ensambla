package com.ensambla.backend.domain.dto.procesamiento;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConfirmarLecturaRequest(
        UUID usuarioId,
        UUID loteId,
        UUID proyectoId,
        String nombreProyecto,
        UUID bloqueId,
        String nombreBloque,
        List<CampoItem> campos,
        List<FilaItem> filas
) {

    public record CampoItem(String clave, UUID campoId, String nombre, String tipo, List<String> opciones) {
    }

    public record FilaItem(Integer pagina, Map<String, Object> valores) {
    }
}
