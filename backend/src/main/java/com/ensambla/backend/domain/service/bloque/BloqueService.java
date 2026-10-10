package com.ensambla.backend.domain.service.bloque;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BloqueService {

    private final BloqueRepository bloqueRepository;

    public Bloque obtener(UUID bloqueId, UUID negocioId) {
        return bloqueRepository.findByIdAndNegocioId(bloqueId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El bloque no existe"));
    }

    /** {@code proyecto} ya viene validado contra el negocio de la sesión y con sus bloques cargados. */
    @Transactional
    public Bloque crear(Proyecto proyecto, String nombre, OffsetDateTime ahora) {
        return bloqueRepository.save(proyecto.agregarBloque(nombre, ahora));
    }

    /** {@code proyecto} es el dueño del bloque, con sus bloques cargados para validar el nombre. */
    @Transactional
    public Bloque renombrar(Proyecto proyecto, Bloque bloque, String nombre, OffsetDateTime ahora) {
        proyecto.validarNombreDeBloqueLibre(nombre, bloque.getId());
        bloque.renombrar(nombre, ahora);
        return bloqueRepository.save(bloque);
    }

    @Transactional
    public void eliminar(Bloque bloque) {
        bloqueRepository.deleteByIdAndNegocioId(bloque.getId(), bloque.getNegocioId());
    }
}
