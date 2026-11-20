package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/** Lo que un resumen del negocio necesita además de los datos del sistema: la zona del negocio y los avisos del sistema. */
@RequiredArgsConstructor
public class ContextoDeResumenService {

    private final NegocioRepository negocioRepository;
    private final ZonaRepository zonaRepository;
    private final AgregadoZonaRepository agregadoZonaRepository;
    private final AvisoRepository avisoRepository;

    /** Los agregados de la zona del negocio, o una zona vacía si todavía no tiene una asignada. */
    public AnalisisZona zonaDe(NegocioId negocioId) {
        return buscarZonaDelNegocio(negocioId)
                .map(this::analizarZona)
                .orElseGet(AnalisisZona::sinZona);
    }

    public List<Aviso> avisosDe(NegocioId negocioId, Proyecto proyecto) {
        return buscarAvisos(negocioId).stream()
                .filter(aviso -> proyecto.getId().equals(aviso.getProyectoId()))
                .toList();
    }

    private Optional<Zona> buscarZonaDelNegocio(NegocioId negocioId) {
        return negocioRepository.findZonaIdById(negocioId).flatMap(zonaRepository::findById);
    }

    private AnalisisZona analizarZona(Zona zona) {
        return agregadoZonaRepository.analizar(zona);
    }

    private List<Aviso> buscarAvisos(NegocioId negocioId) {
        return avisoRepository.findByNegocioId(negocioId);
    }
}
