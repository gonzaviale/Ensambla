package com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.AvisoEconomico;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * El aviso económico del inicio: los últimos 7 días (hoy incluido, en hora argentina) frente a los 7 anteriores. Usa
 * el mismo cálculo que el resumen del inicio y sólo mira el sistema del propio negocio.
 */
@RequiredArgsConstructor
public class ObtenerAvisoEconomico {

    private static final int DIAS_DE_UNA_SEMANA = 7;

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final Clock clock;

    public ObtenerAvisoEconomicoResponse execute(ObtenerAvisoEconomicoRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocio.getId(), request.proyectoId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        DatosDeProyecto datos = datosDeProyectoService.cargar(proyecto, negocio.getId());
        PeriodoResumen semana = new PeriodoResumen(hoy.minusDays(DIAS_DE_UNA_SEMANA - 1L), hoy);
        return ObtenerAvisoEconomicoResponse.de(AvisoEconomico.comparar(
                resumen(negocio, datos, semana, hoy),
                resumen(negocio, datos, semana.anterior(), hoy)));
    }

    private static ResumenNegocio resumen(Negocio negocio, DatosDeProyecto datos, PeriodoResumen periodo,
                                          LocalDate hoy) {
        return ResumenNegocio.armar(negocio.getNombre(), datos, AnalisisZona.sinZona(), List.of(), periodo, hoy);
    }
}
