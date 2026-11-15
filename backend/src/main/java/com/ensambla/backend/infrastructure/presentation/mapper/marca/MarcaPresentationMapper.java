package com.ensambla.backend.infrastructure.presentation.mapper.marca;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.marca.KitDeMarcaItem;
import com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca.GuardarKitDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.obtenerKitDeMarca.ObtenerKitDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarcaRequest;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarcaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.marca.KitDeMarcaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.marca.KitDeMarcaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.marca.LogoSubidoResponse;
import org.springframework.stereotype.Component;

@Component
public class MarcaPresentationMapper {

    public ObtenerKitDeMarcaRequest toObtenerKitRequest(UsuarioId usuarioId) {
        return new ObtenerKitDeMarcaRequest(usuarioId);
    }

    public ObtenerLogoDeMarcaRequest toObtenerLogoRequest(UsuarioId usuarioId) {
        return new ObtenerLogoDeMarcaRequest(usuarioId);
    }

    public GuardarKitDeMarcaRequest toGuardarRequest(UsuarioId usuarioId, KitDeMarcaWebRequest request) {
        return new GuardarKitDeMarcaRequest(
                usuarioId,
                request.colorPrincipal(),
                request.colorSecundario(),
                request.tipografia(),
                request.tono());
    }

    public SubirLogoDeMarcaRequest toSubirLogoRequest(UsuarioId usuarioId, byte[] contenido, String tipoMime) {
        return new SubirLogoDeMarcaRequest(usuarioId, contenido, tipoMime);
    }

    public KitDeMarcaResponse toResponse(KitDeMarcaItem kit) {
        return new KitDeMarcaResponse(
                kit.colorPrincipal(), kit.colorSecundario(), kit.tipografia(), kit.tono(), kit.tieneLogo());
    }

    public LogoSubidoResponse toResponse(SubirLogoDeMarcaResponse subido) {
        return new LogoSubidoResponse(
                toResponse(subido.kit()), subido.colorPrincipalSugerido(), subido.colorSecundarioSugerido());
    }
}
