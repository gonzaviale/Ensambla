package com.ensambla.backend.domain.service.campo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampoService {

    private final CampoRepository campoRepository;

    public List<Campo> listarDeBloque(UUID bloqueId, UUID negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloqueId, negocioId);
    }

    public Campo obtener(UUID campoId, UUID negocioId) {
        return campoRepository.findByIdAndNegocioId(campoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El campo no existe"));
    }

    /** {@code bloque} ya viene validado contra el negocio de la sesión; acá se cargan sus campos para validar. */
    @Transactional
    public Campo crear(Bloque bloque, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        cargarCampos(bloque);
        return campoRepository.save(bloque.agregarCampo(configuracion, ahora));
    }

    /** {@code bloque} es el dueño del campo; acá se cargan sus campos para validar el nombre. */
    @Transactional
    public Campo editar(Bloque bloque, Campo campo, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        cargarCampos(bloque);
        bloque.editarCampo(campo, configuracion, ahora);
        return campoRepository.save(campo);
    }

    @Transactional
    public void eliminar(Campo campo) {
        campoRepository.deleteByIdAndNegocioId(campo.getId(), campo.getNegocioId());
    }

    private void cargarCampos(Bloque bloque) {
        bloque.setCampos(campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), bloque.getNegocioId()));
    }
}
