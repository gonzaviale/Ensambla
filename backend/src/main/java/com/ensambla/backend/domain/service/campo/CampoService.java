package com.ensambla.backend.domain.service.campo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.Formula;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CampoService {

    private final CampoRepository campoRepository;

    private final BloqueRepository bloqueRepository;

    private final TransaccionPort transaccionPort;

    public List<Campo> listarDeBloque(UUID bloqueId, UUID negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloqueId, negocioId);
    }

    public Campo obtener(UUID campoId, UUID negocioId) {
        return campoRepository.findByIdAndNegocioId(campoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El campo no existe"));
    }

    /** {@code bloque} ya viene validado contra el negocio de la sesión; acá se cargan sus campos para validar. */
    public Campo crear(Bloque bloque, ConfiguracionCampo configuracion, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            cargarCampos(bloque);
            Campo campo = bloque.agregarCampo(configuracion, ahora);
            validarConfiguracion(bloque, campo);
            return campoRepository.save(campo);
        });
    }

    /** Valida la conexión y la fórmula del campo contra su bloque, que ya tiene sus campos cargados. */
    public void validarConfiguracion(Bloque bloque, Campo campo) {
        validarBloqueRelacionado(bloque, campo);
        validarReferenciasDeFormula(bloque, campo);
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

    /**
     * Una fórmula sólo puede usar campos NUMERO del mismo bloque; como no puede citar a otra fórmula, tampoco puede
     * haber referencias circulares.
     */
    private void validarReferenciasDeFormula(Bloque bloque, Campo campo) {
        if (!campo.esFormula()) {
            return;
        }
        Set<UUID> numericos = bloque.getCampos().stream()
                .filter(otro -> otro.getTipo() == TipoCampo.NUMERO && !otro.getId().equals(campo.getId()))
                .map(Campo::getId)
                .collect(Collectors.toSet());
        boolean validas = numericos.containsAll(Formula.parsear(campo.getFormula()).referencias());
        if (!validas) {
            throw new CampoInvalidoException(Map.of("formula", "Usá sólo campos numéricos de esta tabla"));
        }
    }

    /** No se puede dejar sin uso o cambiar un campo numérico del que depende una fórmula. */
    public void validarSinFormulasQueLoUsen(List<Campo> camposDelBloque, Campo campo, String accion) {
        camposDelBloque.stream()
                .filter(Campo::esFormula)
                .filter(formula -> Formula.parsear(formula.getFormula()).referencias().contains(campo.getId()))
                .findFirst()
                .ifPresent(formula -> {
                    throw new BusinessRuleException("No se puede " + accion + " «" + campo.getNombre()
                            + "»: la fórmula «" + formula.getNombre() + "» lo usa");
                });
    }

    public void cargarCampos(Bloque bloque) {
        bloque.setCampos(campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), bloque.getNegocioId()));
    }
}
