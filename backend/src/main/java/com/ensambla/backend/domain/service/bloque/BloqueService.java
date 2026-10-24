package com.ensambla.backend.domain.service.bloque;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class BloqueService {

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    public Bloque obtener(UUID bloqueId, UUID negocioId) {
        return bloqueRepository.findByIdAndNegocioId(bloqueId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El bloque no existe"));
    }

    /** {@code proyecto} ya viene validado contra el negocio de la sesión y con sus bloques cargados. */
    public Bloque crear(Proyecto proyecto, String nombre, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            return bloqueRepository.save(proyecto.agregarBloque(nombre, ahora));
        });
    }

    /** {@code proyecto} es el dueño del bloque, con sus bloques cargados para validar el nombre. */
    public Bloque renombrar(Proyecto proyecto, Bloque bloque, String nombre, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            proyecto.validarNombreDeBloqueLibre(nombre, bloque.getId());
            bloque.renombrar(nombre, ahora);
            return bloqueRepository.save(bloque);
        });
    }

    /** No se puede borrar una tabla que otra tabla usa en una conexión; la autorreferencia no cuenta. */
    public void eliminar(Bloque bloque) {
        transaccionPort.ejecutar(() -> {
            validarSinConexionesHacia(bloque);
            bloqueRepository.deleteByIdAndNegocioId(bloque.getId(), bloque.getNegocioId());
        });
    }

    private void validarSinConexionesHacia(Bloque bloque) {
        List<Campo> conexiones = campoRepository.findRelacionesHacia(bloque.getId(), bloque.getNegocioId());
        if (conexiones.isEmpty()) {
            return;
        }
        Campo campo = conexiones.getFirst();
        Bloque otro = obtener(campo.getBloqueId(), bloque.getNegocioId());
        throw new BusinessRuleException("La tabla «" + bloque.getNombre() + "» se usa en la columna «"
                + campo.getNombre() + "» de «" + otro.getNombre() + "»");
    }
}
