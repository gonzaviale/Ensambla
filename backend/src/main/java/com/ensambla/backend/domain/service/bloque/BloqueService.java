package com.ensambla.backend.domain.service.bloque;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class BloqueService {

    private final BloqueRepository bloqueRepository;

    private final TransaccionPort transaccionPort;

    public Bloque obtener(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.findByIdAndNegocioId(bloqueId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El bloque no existe"));
    }

    /** {@code proyecto} ya viene validado contra el negocio de la sesión y con sus bloques cargados. */
    public Bloque crear(Proyecto proyecto, String nombre, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            return bloqueRepository.save(proyecto.agregarBloque(nombre, ahora));
        });
    }
}
