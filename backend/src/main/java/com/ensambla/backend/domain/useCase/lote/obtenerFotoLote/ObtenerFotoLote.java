package com.ensambla.backend.domain.useCase.lote.obtenerFotoLote;

import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerFotoLote {

    private final NegocioService negocioService;
    private final LoteSubidaService loteSubidaService;

    public FotoArchivoResponse execute(ObtenerFotoLoteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        ArchivoFoto foto = loteSubidaService.leerFotoDeLote(negocioId, request.loteId(), request.fotoId());
        return FotoArchivoResponse.de(foto);
    }
}
