package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
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
 * El resumen conjunto de la cuenta: todos los negocios del usuario, cada uno con las mismas cuentas que «Cómo viene
 * tu negocio» (los últimos 30 días) y sus avisos pendientes. No depende del negocio activo. Los datos se cargan
 * negocio por negocio, como en «Cómo viene tu negocio», porque el plan limita la cuenta a pocos negocios; los avisos
 * se cuentan de una vez para todos.
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
        List<Negocio> negocios = negocioRepository.findAllByDuenioId(request.usuarioId());
        Map<NegocioId, Long> pendientes = avisoRepository.contarPorNegocio(
                negocios.stream().map(Negocio::getId).toList(), EstadoAviso.PENDIENTE);
        List<NegocioResumido> resumidos = negocios.stream()
                .map(negocio -> resumir(negocio, hoy, pendientes.getOrDefault(negocio.getId(), 0L)))
                .toList();
        return ObtenerResumenDeLaCuentaResponse.de(
                PeriodoResumen.ultimosDias(ComoVieneElNegocio.DIAS_DEL_PERIODO, hoy), resumidos);
    }

    private NegocioResumido resumir(Negocio negocio, LocalDate hoy, long avisosPendientes) {
        List<DatosDeProyecto> datos = sistemaRepository.findAllByNegocioId(negocio.getId()).stream()
                .map(datosDeProyectoService::cargar)
                .toList();
        return NegocioResumido.de(negocio, ComoVieneElNegocio.armar(datos, hoy), avisosPendientes);
    }
}
