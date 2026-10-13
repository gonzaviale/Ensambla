package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.DireccionOrden;

import java.util.List;
import java.util.UUID;

/**
 * Qué registros de un bloque se piden: texto a buscar entre los valores, campo y dirección del orden, y página.
 * {@code campoOrdenId} nulo significa ordenar por fecha de carga.
 */
public record ConsultaRegistros(
        UUID negocioId,
        UUID bloqueId,
        String texto,
        UUID campoOrdenId,
        DireccionOrden direccion,
        int pagina,
        int tamanio) {

    public static final int TAMANIO_POR_DEFECTO = 20;
    public static final int TAMANIO_MAXIMO = 100;

    private static final String ORDEN_POR_CARGA = "creadoEn";

    public static ConsultaRegistros armar(UUID negocioId, UUID bloqueId, List<Campo> campos, String texto,
                                          String orden, String direccion, Integer pagina, Integer tamanio) {
        return new ConsultaRegistros(
                negocioId,
                bloqueId,
                textoDe(texto),
                campoOrdenDe(orden, campos),
                DireccionOrden.desde(direccion),
                paginaDe(pagina),
                tamanioDe(tamanio));
    }

    private static String textoDe(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static UUID campoOrdenDe(String orden, List<Campo> campos) {
        if (orden == null || orden.isBlank() || ORDEN_POR_CARGA.equals(orden.strip())) {
            return null;
        }
        String buscado = orden.strip();
        return campos.stream()
                .filter(campo -> !campo.esFormula())
                .map(Campo::getId)
                .filter(id -> id.toString().equals(buscado))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No se puede ordenar por ese campo"));
    }

    private static int paginaDe(Integer pagina) {
        return pagina == null ? 0 : Math.max(0, pagina);
    }

    private static int tamanioDe(Integer tamanio) {
        return tamanio == null ? TAMANIO_POR_DEFECTO : Math.min(TAMANIO_MAXIMO, Math.max(1, tamanio));
    }
}
