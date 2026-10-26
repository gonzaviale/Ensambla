package com.ensambla.backend.infrastructure.presentation.mapper.lote;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.lote.LoteItem.FotoItem;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import com.ensambla.backend.domain.useCase.lote.obtenerFotoLote.ObtenerFotoLoteRequest;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLoteRequest;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLoteRequest;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse.FotoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class LotePresentationMapper {

    public SubirLoteRequest toSubirRequest(UsuarioId usuarioId, List<MultipartFile> fotos, BloqueId bloqueDestinoId) {
        return new SubirLoteRequest(usuarioId, fotos.stream().map(this::contenido).toList(), bloqueDestinoId);
    }

    public ObtenerLoteRequest toObtenerRequest(UsuarioId usuarioId, LoteId loteId) {
        return new ObtenerLoteRequest(usuarioId, loteId);
    }

    public ObtenerFotoLoteRequest toFotoRequest(UsuarioId usuarioId, LoteId loteId, FotoId fotoId) {
        return new ObtenerFotoLoteRequest(usuarioId, loteId, fotoId);
    }

    public LoteResponse toResponse(LoteItem lote) {
        return new LoteResponse(
                lote.id(),
                lote.estado(),
                lote.creadoEn(),
                lote.fotos().stream().map(this::toResponse).toList());
    }

    private FotoResponse toResponse(FotoItem foto) {
        return new FotoResponse(foto.id(), foto.posicion(), foto.tipoContenido(), foto.tamanioBytes());
    }

    private byte[] contenido(MultipartFile foto) {
        try {
            return foto.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la foto recibida", e);
        }
    }
}
