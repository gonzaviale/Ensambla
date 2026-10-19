package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.UbicacionNegocio;
import com.ensambla.backend.domain.model.enums.RubroNegocio;
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
        negocio.configurar(nombre, rubro, ubicacion, comparteDatosZona, ahora);
        return negocioRepository.save(negocio);
    }
}

