package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.reporte.PanelNegocio;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * El resumen conjunto de la cuenta ({@code GET /api/negocios/resumen}, ver {@code docs/negocios-de-la-cuenta.md}):
 * todos los negocios del usuario, cada uno con las métricas del panel de la home sumadas sobre sus sistemas y sus
 * avisos pendientes. Los datos se cargan negocio por negocio, porque el plan limita la cuenta a pocos negocios; los
 * avisos se cuentan de una vez para todos. El activo es el negocio con el que trabaja la sesión.
 */
@RequiredArgsConstructor
public class ObtenerResumenDeLaCuenta {

    private final NegocioRepository negocioRepository;

    private final SistemaRepository sistemaRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final AvisoRepository avisoRepository;

    private final Clock clock;

    public ObtenerResumenDeLaCuentaResponse execute(ObtenerResumenDeLaCuentaRequest request) {
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        PeriodoResumen periodo = PeriodoResumen.ultimosDias(request.dias(), hoy);
        NegocioId activoId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        List<Negocio> negocios = negocioRepository.findAllByDuenioId(request.usuarioId());
        Map<NegocioId, Long> pendientes = avisoRepository.contarPorNegocio(
                negocios.stream().map(Negocio::getId).toList(), EstadoAviso.PENDIENTE);
        List<NegocioResumido> resumidos = negocios.stream()
                .map(negocio -> NegocioResumido.de(
                        negocio,
                        negocio.getId().equals(activoId),
                        paneles(negocio, periodo, hoy),
                        pendientes.getOrDefault(negocio.getId(), 0L)))
                .toList();
        return ObtenerResumenDeLaCuentaResponse.de(periodo, resumidos);
    }

    private List<PanelNegocio> paneles(Negocio negocio, PeriodoResumen periodo, LocalDate hoy) {
        return sistemaRepository.findAllByNegocioId(negocio.getId()).stream()
                .map(sistema -> PanelNegocio.armar(datosDeProyectoService.cargar(sistema), periodo, hoy))
                .toList();
    }
}
