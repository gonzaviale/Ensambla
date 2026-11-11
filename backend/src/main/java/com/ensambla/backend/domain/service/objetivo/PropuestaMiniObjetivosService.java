package com.ensambla.backend.domain.service.objetivo;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.CandidatosMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Carga lo que el negocio tiene en todos sus cuadernos y en su zona y arma los mini-objetivos posibles. Lo comparten
 * los casos de uso que proponen, ofrecen o cambian mini-objetivos: todos tienen que ver los mismos candidatos.
 */
@RequiredArgsConstructor
public class PropuestaMiniObjetivosService {

    private final NegocioRepository negocioRepository;

    private final SistemaRepository sistemaRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final ZonaRepository zonaRepository;

    private final AgregadoZonaRepository agregadoZonaRepository;

    private final MetricasVentas metricasVentas;

    /** El resultado del negocio hoy y los mini-objetivos posibles, del que más suma al que menos. */
    public CandidatosDelNegocio candidatos(NegocioId negocioId, LocalDate hoy) {
        List<DatosDeProyecto> datos = sistemaRepository.findAllByNegocioId(negocioId).stream()
                .map(datosDeProyectoService::cargar)
                .toList();
        MedidorMiniObjetivo medidor = MedidorMiniObjetivo.de(datos, metricasVentas, hoy);
        return new CandidatosDelNegocio(medidor.resultado(), CandidatosMiniObjetivo.armar(medidor, zonaDe(negocioId)));
    }

    private AnalisisZona zonaDe(NegocioId negocioId) {
        return negocioRepository.findZonaIdById(negocioId)
                .flatMap(zonaRepository::findById)
                .map(agregadoZonaRepository::analizar)
                .orElseGet(AnalisisZona::sinZona);
    }
}
