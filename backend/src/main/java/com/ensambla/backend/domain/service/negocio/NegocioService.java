package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.enums.RubroNegocio;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NegocioService {

    private final NegocioRepository negocioRepository;
    private final ZonaService zonaService;
    private final UbicacionService ubicacionService;

    public List<UUID> listarIds() {
        return negocioRepository.findAllIds();
    }

    public UUID obtenerDelUsuario(UUID usuarioId) {
        return negocioRepository.findIdByDuenioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el negocio del usuario"));
    }

    public Negocio obtenerConfiguracion(UUID usuarioId) {
        return negocioRepository.findByDuenioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el negocio del usuario"));
    }

    @Transactional
    public Negocio actualizarConfiguracion(UUID usuarioId, String nombre, RubroNegocio rubro,
                                           UbicacionNegocio ubicacion, Boolean comparteDatosZona,
                                           OffsetDateTime ahora) {
        Negocio negocio = negocioRepository.findByDuenioIdParaActualizar(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el negocio del usuario"));
        if (ubicacion != null && negocio.cambiaUbicacion(ubicacion)) {
            ubicacionService.exigirLocalidadDelPunto(
                    ubicacion.localidad(), ubicacion.latitud(), ubicacion.longitud(), ahora.toInstant());
        }
        negocio.configurar(nombre, rubro, ubicacion, comparteDatosZona, ahora);
        if (negocio.necesitaZona()) {
            negocio.asignarZona(zonaService.resolver(negocio.getLatitud(), negocio.getLongitud()).orElse(null));
        }
        return negocioRepository.save(negocio);
    }
}

