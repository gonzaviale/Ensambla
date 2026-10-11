package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.dto.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.dto.lote.ObtenerFotoLoteRequest;
import com.ensambla.backend.domain.mapper.lote.FotoArchivoMapper;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerFotoLoteUseCase {

    private final NegocioService negocioService;
    private final LoteSubidaService loteSubidaService;
    private final FotoArchivoMapper fotoArchivoMapper;

    public FotoArchivoResponse execute(ObtenerFotoLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        ArchivoFoto foto = loteSubidaService.leerFotoDeLote(negocioId, request.loteId(), request.fotoId());
        return fotoArchivoMapper.toResponse(foto);
    }
}
