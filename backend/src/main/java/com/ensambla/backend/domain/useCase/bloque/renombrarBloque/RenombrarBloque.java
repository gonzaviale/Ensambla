package com.ensambla.backend.domain.useCase.bloque.renombrarBloque;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class RenombrarBloque {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final BloqueRepository bloqueRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public BloqueItem execute(RenombrarBloqueRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        Proyecto proyecto = obtenerProyecto(negocioId, bloque.getProyectoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return BloqueItem.de(renombrarEnTransaccion(proyecto, bloque, request.nombre(), ahora));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.obtener(bloqueId, negocioId);
    }

    private Proyecto obtenerProyecto(NegocioId negocioId, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocioId, proyectoId);
    }

    private Bloque renombrarEnTransaccion(Proyecto proyecto, Bloque bloque, String nombre, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> renombrar(proyecto, bloque, nombre, ahora));
    }

    /** {@code proyecto} es el dueño del bloque, con sus bloques cargados para validar el nombre. */
    private Bloque renombrar(Proyecto proyecto, Bloque bloque, String nombre, OffsetDateTime ahora) {
        proyecto.validarNombreDeBloqueLibre(nombre, bloque.getId());
        bloque.renombrar(nombre, ahora);
        return guardar(bloque);
    }

    private Bloque guardar(Bloque bloque) {
        return bloqueRepository.save(bloque);
    }
}
