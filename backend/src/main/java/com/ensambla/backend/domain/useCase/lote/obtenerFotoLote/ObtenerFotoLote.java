package com.ensambla.backend.domain.useCase.lote.obtenerFotoLote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
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
        FotoCuaderno foto = fotoDelLote(negocioId, request.loteId(), request.fotoId());
        ArchivoFoto archivo = loteSubidaService.leer(negocioId, foto);
        return FotoArchivoResponse.de(archivo);
    }

    /** Si el lote o la foto no son del negocio responde como inexistente. */
    private FotoCuaderno fotoDelLote(UUID negocioId, UUID loteId, UUID fotoId) {
        return loteSubidaService.obtener(negocioId, loteId).getFotos().stream()
                .filter(candidata -> candidata.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
    }
}
