package com.ensambla.backend.domain.useCase.bloque.renombrarBloque;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class RenombrarBloque {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final ProyectoService proyectoService;

    private final BloqueRepository bloqueRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public BloqueItem execute(RenombrarBloqueRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        Proyecto proyecto = proyectoService.obtener(negocioId, bloque.getProyectoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return BloqueItem.de(transaccionPort.ejecutar(() -> renombrar(proyecto, bloque, request.nombre(), ahora)));
    }

    /** {@code proyecto} es el dueño del bloque, con sus bloques cargados para validar el nombre. */
    private Bloque renombrar(Proyecto proyecto, Bloque bloque, String nombre, OffsetDateTime ahora) {
        proyecto.validarNombreDeBloqueLibre(nombre, bloque.getId());
        bloque.renombrar(nombre, ahora);
        return bloqueRepository.save(bloque);
    }
}
