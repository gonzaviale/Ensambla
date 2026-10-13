package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Qué filas entran en un reporte de un bloque: las que cumplen el texto, se cargaron entre las dos fechas (inclusive)
 * y tienen cierto valor en una columna de opción o de sí/no. Todos los filtros son opcionales. {@code desde} y
 * {@code hasta} son instantes: el comienzo del día de la primera fecha (incluido) y el comienzo del día siguiente a la
 * última (excluido), en la zona horaria de la persona.
 */
public record FiltroReporte(
        UUID negocioId,
        UUID bloqueId,
        String texto,
        Instant desde,
        Instant hasta,
        UUID campoId,
        String valor) {

    /** Las fechas de los reportes son las del día en Argentina, no las del servidor. */
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    /**
     * @param campos los del bloque: el filtro por valor sólo admite una columna de opción o de sí/no
     * @throws BusinessRuleException si las fechas están al revés, la columna no se puede filtrar o falta el valor
     */
    public static FiltroReporte armar(
            UUID negocioId,
            UUID bloqueId,
            List<Campo> campos,
            String texto,
            LocalDate desde,
            LocalDate hasta,
            UUID campoId,
            String valor) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessRuleException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        return new FiltroReporte(
                negocioId,
                bloqueId,
                texto == null || texto.isBlank() ? null : texto.strip(),
                desde == null ? null : desde.atStartOfDay(ZONA).toInstant(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant(),
                campoId,
                campoId == null ? null : valorDe(campos, campoId, valor));
    }

    public boolean filtraPorColumna() {
        return campoId != null;
    }

    private static String valorDe(List<Campo> campos, UUID campoId, String valor) {
        Campo campo = campos.stream()
                .filter(candidato -> candidato.getId().equals(campoId))
                .filter(candidato -> candidato.getTipo() == TipoCampo.OPCION
                        || candidato.getTipo() == TipoCampo.SI_NO)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No se puede filtrar por ese campo"));
        if (valor == null || valor.isBlank()) {
            throw new BusinessRuleException("Elegí el valor por el que filtrar");
        }
        boolean valido = campo.getTipo() == TipoCampo.SI_NO
                ? "true".equals(valor) || "false".equals(valor)
                : campo.getOpciones().contains(valor);
        if (!valido) {
            throw new BusinessRuleException("Elegí un valor de la columna «" + campo.getNombre() + "»");
        }
        return valor;
    }
}
