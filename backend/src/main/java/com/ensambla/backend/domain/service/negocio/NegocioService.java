package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.ubicacion.UbicacionService;
import com.ensambla.backend.domain.service.zona.ZonaService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class NegocioService {

    private final NegocioRepository negocioRepository;
    private final ZonaService zonaService;
    private final UbicacionService ubicacionService;

    private final TransaccionPort transaccionPort;

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

    public Negocio actualizarConfiguracion(UUID usuarioId, String nombre, RubroNegocio rubro,
                                           UbicacionNegocio ubicacion, Boolean comparteDatosZona,
                                           OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
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
        });
    }
}

