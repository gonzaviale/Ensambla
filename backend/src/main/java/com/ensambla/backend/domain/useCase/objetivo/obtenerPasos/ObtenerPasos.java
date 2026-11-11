package com.ensambla.backend.domain.useCase.objetivo.obtenerPasos;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.PasoObjetivo;
import com.ensambla.backend.domain.model.objetivo.PasosObjetivo;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ObtenerPasos {

    private final NegocioRepository negocioRepository;

    private final SistemaRepository sistemaRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final ZonaRepository zonaRepository;

    private final AgregadoZonaRepository agregadoZonaRepository;

    private final MetricasVentas metricasVentas;

    private final Clock clock;

    public ObtenerPasosResponse execute(ObtenerPasosRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        List<DatosDeProyecto> datos = sistemaRepository.findAllByNegocioId(negocioId).stream()
                .map(datosDeProyectoService::cargar)
                .toList();
        List<PasoObjetivo> pasos = PasosObjetivo.elegir(
                datos,
                metricasVentas,
                ZonaNegocio.dia(OffsetDateTime.now(clock)),
                zonaDe(negocioId));
        return ObtenerPasosResponse.de(pasos);
    }

    private AnalisisZona zonaDe(NegocioId negocioId) {
        return negocioRepository.findZonaIdById(negocioId)
                .flatMap(zonaRepository::findById)
                .map(agregadoZonaRepository::analizar)
                .orElseGet(AnalisisZona::sinZona);
    }
}
