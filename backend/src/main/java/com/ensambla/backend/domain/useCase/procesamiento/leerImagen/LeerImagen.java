package com.ensambla.backend.domain.useCase.procesamiento.leerImagen;

import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ConsumoIaInmediatoService;
import com.ensambla.backend.domain.service.procesamiento.LecturaImagenService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class LeerImagen {

    private final LecturaImagenService lecturaImagenService;
    /** Debajo de este valor de confianza un dato se marca como dudoso. */
    private final double umbralConfianza;
    private final NegocioService negocioService;
    private final ConsumoIaInmediatoService consumoIaInmediatoService;
    private final Clock clock;

    public LeerImagenItem execute(LeerImagenRequest request) {
        ImagenCuaderno imagen = request.aImagenCuaderno();
        BloqueDeImagen bloque = request.aBloqueDeImagen();
        registrarConsumo(request.usuarioId());
        LecturaImagen lectura = lecturaImagenService.leer(imagen, bloque, request.destinoExistente());
        return LeerImagenItem.de(lectura, umbralConfianza);
    }

    private void registrarConsumo(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        consumoIaInmediatoService.registrar(negocioId, OffsetDateTime.now(clock));
    }
}
