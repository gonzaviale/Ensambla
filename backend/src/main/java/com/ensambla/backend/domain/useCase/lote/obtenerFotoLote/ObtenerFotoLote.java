package com.ensambla.backend.domain.useCase.lote.obtenerFotoLote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerFotoLote {

    private final NegocioRepository negocioRepository;
    private final ArchivoCuadernoPort archivoCuadernoPort;

    private final LoteSubidaRepository loteSubidaRepository;

    public FotoArchivoResponse execute(ObtenerFotoLoteRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        FotoCuaderno foto = obtenerFotoDelLote(negocioId, request.loteId(), request.fotoId());
        ArchivoFoto archivo = leerArchivo(negocioId, foto);
        return FotoArchivoResponse.de(archivo);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    /** Si el lote o la foto no son del negocio responde como inexistente. */
    private FotoCuaderno obtenerFotoDelLote(NegocioId negocioId, LoteId loteId, FotoId fotoId) {
        return obtenerLote(negocioId, loteId).fotoConId(fotoId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
    }

    private LoteSubida obtenerLote(NegocioId negocioId, LoteId loteId) {
        return loteSubidaRepository.obtener(negocioId, loteId);
    }

    private ArchivoFoto leerArchivo(NegocioId negocioId, FotoCuaderno foto) {
        return archivoCuadernoPort.leerFoto(negocioId, foto);
    }
}
