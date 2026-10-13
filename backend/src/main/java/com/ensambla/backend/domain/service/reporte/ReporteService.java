package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    /** Filas que se muestran en pantalla antes de descargar. */
    public static final int MAXIMO_VISTA = 50;

    /** Filas que entran en un archivo descargado. */
    public static final int MAXIMO_EXPORTACION = 10_000;

    private final RegistroRepository registroRepository;

    private final CampoRepository campoRepository;

    /**
     * Las primeras {@code limite} filas que cumplen el filtro (con el valor de cada fórmula calculado) y los
     * totales de todas: cuántas son y cuánto suma cada columna numérica.
     */
    public ReporteRegistros generar(FiltroReporte filtro, List<Campo> campos, int limite) {
        List<Registro> filas = registroRepository.buscarFiltrados(filtro, limite).stream()
                .map(fila -> fila.conFormulas(campos))
                .toList();
        long cantidad = registroRepository.contarFiltrados(filtro);
        List<UUID> numericos = campos.stream()
                .filter(campo -> campo.getTipo() == TipoCampo.NUMERO)
                .map(Campo::getId)
                .toList();
        Map<UUID, BigDecimal> sumas = registroRepository.sumarNumericos(filtro, numericos);
        return new ReporteRegistros(filas, TotalesRegistros.armar(cantidad, numericos, sumas), cantidad > limite);
    }

    /**
     * El nombre de cada fila a la que apuntan las columnas de conexión de las filas del reporte (una consulta por
     * tabla conectada), indexado por el id de la fila.
     */
    public Map<UUID, String> etiquetasDeConexiones(UUID negocioId, List<Campo> campos, List<Registro> filas) {
        Map<UUID, Set<UUID>> idsPorTabla = new HashMap<>();
        for (Campo campo : campos.stream().filter(Campo::esRelacion).toList()) {
            for (Registro fila : filas) {
                if (fila.getValores().get(campo.getId().toString()) instanceof String id) {
                    idsPorTabla.computeIfAbsent(campo.getBloqueRelacionadoId(), tabla -> new HashSet<>())
                            .add(UUID.fromString(id));
                }
            }
        }
        Map<UUID, String> etiquetas = new HashMap<>();
        idsPorTabla.forEach((tablaId, ids) -> {
            Function<Registro, Referencia> etiquetador = Referencia.etiquetador(
                    campoRepository.findByBloqueIdAndNegocioId(tablaId, negocioId));
            registroRepository.findAllByIdsAndBloque(ids, tablaId, negocioId).stream()
                    .map(etiquetador)
                    .forEach(referencia -> etiquetas.put(referencia.id(), referencia.etiqueta()));
        });
        return etiquetas;
    }
}
