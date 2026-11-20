package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Qué filas entran en un reporte de un bloque: las que cumplen el texto, se cargaron entre las dos fechas (inclusive)
 * y tienen cierto valor en una columna de opción o de sí/no. Todos los filtros son opcionales. {@code desde} y
 * {@code hasta} son instantes: el comienzo del día de la primera fecha (incluido) y el comienzo del día siguiente a la
 * última (excluido), en la zona horaria de la persona.
 */
public record FiltroReporte(
        NegocioId negocioId,
        BloqueId bloqueId,
        String texto,
        Instant desde,
        Instant hasta,
        CampoId campoId,
        String valor) {

    /** Las fechas de los reportes son las del día en Argentina, no las del servidor. */
    public static final ZoneId ZONA = ZonaNegocio.ZONA;

    /**
     * @param campos los del bloque: el filtro por valor sólo admite una columna de opción o de sí/no
     * @throws BusinessRuleException si las fechas están al revés, la columna no se puede filtrar o falta el valor
     */
    public static FiltroReporte armar(
            NegocioId negocioId,
            BloqueId bloqueId,
            List<Campo> campos,
            String texto,
            LocalDate desde,
            LocalDate hasta,
            CampoId campoId,
            String valor) {
        exigirQueLasFechasEstenEnOrden(desde, hasta);
        return new FiltroReporte(
                negocioId, bloqueId, textoLimpio(texto), comienzoDe(desde), finDe(hasta), campoId,
                valorDelFiltro(campos, campoId, valor));
    }

    public boolean filtraPorColumna() {
        return campoId != null;
    }

    private static void exigirQueLasFechasEstenEnOrden(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessRuleException("La fecha desde no puede ser posterior a la fecha hasta");
        }
    }

    private static String textoLimpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static Instant comienzoDe(LocalDate dia) {
        return dia == null ? null : dia.atStartOfDay(ZONA).toInstant();
    }

    /** El hasta es inclusive: el filtro termina donde empieza el día siguiente. */
    private static Instant finDe(LocalDate dia) {
        return dia == null ? null : comienzoDe(dia.plusDays(1));
    }

    private static String valorDelFiltro(List<Campo> campos, CampoId campoId, String valor) {
        return campoId == null ? null : valorDe(campos, campoId, valor);
    }

    private static String valorDe(List<Campo> campos, CampoId campoId, String valor) {
        Campo campo = campoFiltrable(campos, campoId);
        exigirUnValor(valor);
        exigirQueSeaUnValorDe(campo, valor);
        return valor;
    }

    private static Campo campoFiltrable(List<Campo> campos, CampoId campoId) {
        return campos.stream()
                .filter(candidato -> candidato.getId().equals(campoId))
                .filter(Campo::admiteFiltroPorValor)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No se puede filtrar por ese dato"));
    }

    private static void exigirUnValor(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessRuleException("Elegí el valor por el que filtrar");
        }
    }

    private static void exigirQueSeaUnValorDe(Campo campo, String valor) {
        if (!campo.admiteFiltrarPor(valor)) {
            throw new BusinessRuleException("Elegí un valor de la columna «" + campo.getNombre() + "»");
        }
    }
}
