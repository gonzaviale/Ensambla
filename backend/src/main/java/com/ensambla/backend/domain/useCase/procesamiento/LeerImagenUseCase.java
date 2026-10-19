package com.ensambla.backend.domain.useCase.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.LeerImagenItem;
import com.ensambla.backend.domain.dto.procesamiento.LeerImagenRequest;
import com.ensambla.backend.domain.mapper.procesamiento.LeerImagenMapper;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.domain.service.procesamiento.LecturaImagenService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LeerImagenUseCase {

    private final LecturaImagenService lecturaImagenService;
    private final LeerImagenMapper leerImagenMapper;

    public LeerImagenItem execute(LeerImagenRequest request) {
        ImagenCuaderno imagen = leerImagenMapper.toImagen(request);
        BloqueDeImagen bloque = leerImagenMapper.toBloque(request);
        LecturaImagen lectura = lecturaImagenService.leer(imagen, bloque, request.destinoExistente());
        return leerImagenMapper.toResponse(lectura);
    }
}
