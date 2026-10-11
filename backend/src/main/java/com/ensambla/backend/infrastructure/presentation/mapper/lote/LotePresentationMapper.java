package com.ensambla.backend.infrastructure.presentation.mapper.lote;

import com.ensambla.backend.domain.dto.lote.LoteItem;
import com.ensambla.backend.domain.dto.lote.LoteItem.FotoItem;
import com.ensambla.backend.domain.dto.lote.ObtenerLoteRequest;
import com.ensambla.backend.domain.dto.lote.SubirLoteRequest;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse.FotoResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@Component
public class LotePresentationMapper {

    public SubirLoteRequest toSubirRequest(UUID usuarioId, List<MultipartFile> fotos) {
        return new SubirLoteRequest(usuarioId, fotos.stream().map(this::contenido).toList());
    }

    public ObtenerLoteRequest toObtenerRequest(UUID usuarioId, UUID loteId) {
        return new ObtenerLoteRequest(usuarioId, loteId);
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
