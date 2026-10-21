package com.ensambla.backend.domain.useCase.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenRequest;
import com.ensambla.backend.domain.mapper.procesamiento.LeerImagenMapper;
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
public class LeerImagenUseCase {

    private final LecturaImagenService lecturaImagenService;
    private final LeerImagenMapper leerImagenMapper;
    private final NegocioService negocioService;
    private final ConsumoIaInmediatoService consumoIaInmediatoService;
    private final Clock clock;

    public LeerImagenItem execute(LeerImagenRequest request) {
        ImagenCuaderno imagen = leerImagenMapper.toImagen(request);
        BloqueDeImagen bloque = leerImagenMapper.toBloque(request);
        registrarConsumo(request.usuarioId());
        LecturaImagen lectura = lecturaImagenService.leer(imagen, bloque, request.destinoExistente());
        return leerImagenMapper.toResponse(lectura);
    }

    private void registrarConsumo(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        consumoIaInmediatoService.registrar(negocioId, OffsetDateTime.now(clock));
    }
}
