package com.ensambla.backend.domain.useCase.lote.obtenerFotoLote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerFotoLote {

    private final NegocioService negocioService;
    private final LoteSubidaService loteSubidaService;

    public FotoArchivoResponse execute(ObtenerFotoLoteRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        FotoCuaderno foto = fotoDelLote(negocioId, request.loteId(), request.fotoId());
        ArchivoFoto archivo = loteSubidaService.leer(negocioId, foto);
        return FotoArchivoResponse.de(archivo);
    }

    /** Si el lote o la foto no son del negocio responde como inexistente. */
    private FotoCuaderno fotoDelLote(NegocioId negocioId, LoteId loteId, FotoId fotoId) {
        return loteSubidaService.obtener(negocioId, loteId).getFotos().stream()
                .filter(candidata -> candidata.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
    }
}
