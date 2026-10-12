package com.ensambla.backend.domain.service.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampoService {

    private final CampoRepository campoRepository;

    private final BloqueRepository bloqueRepository;

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
        Campo campo = bloque.agregarCampo(configuracion, ahora);
        validarBloqueRelacionado(bloque, campo);
        return campoRepository.save(campo);
    }

    /** {@code bloque} es el dueño del campo; acá se cargan sus campos para validar el nombre. */
    @Transactional
    public Campo editar(Bloque bloque, Campo campo, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        cargarCampos(bloque);
        bloque.editarCampo(campo, configuracion, ahora);
        validarBloqueRelacionado(bloque, campo);
        return campoRepository.save(campo);
    }

    @Transactional
    public void eliminar(Campo campo) {
        campoRepository.deleteByIdAndNegocioId(campo.getId(), campo.getNegocioId());
    }

    /**
     * La tabla con la que se conecta un campo RELACION tiene que ser del mismo sistema (proyecto) que su bloque; puede
     * ser el propio bloque. Una tabla de otro negocio responde igual que una inexistente.
     */
    private void validarBloqueRelacionado(Bloque bloque, Campo campo) {
        if (!campo.esRelacion()) {
            return;
        }
        boolean delMismoProyecto = bloqueRepository
                .findByIdAndNegocioId(campo.getBloqueRelacionadoId(), bloque.getNegocioId())
                .filter(relacionado -> relacionado.getProyectoId().equals(bloque.getProyectoId()))
                .isPresent();
        if (!delMismoProyecto) {
            throw new CampoInvalidoException(Map.of("bloqueRelacionadoId", "Elegí una tabla de este sistema"));
        }
    }

    private void cargarCampos(Bloque bloque) {
        bloque.setCampos(campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), bloque.getNegocioId()));
    }
}
