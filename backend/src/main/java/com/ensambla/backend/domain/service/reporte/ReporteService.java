package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ReporteService {

    private final RegistroRepository registroRepository;

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
}
