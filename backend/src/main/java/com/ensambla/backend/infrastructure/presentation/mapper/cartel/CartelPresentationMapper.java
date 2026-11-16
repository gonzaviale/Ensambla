package com.ensambla.backend.infrastructure.presentation.mapper.cartel;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartelRequest;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartelResponse;
import com.ensambla.backend.domain.useCase.cartel.TextosDeCartelItem;
import com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel.RedactarTextosDeCartelRequest;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.OpcionesDeCartelResponse;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.OpcionesDeCartelWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.TextosDeCartelResponse;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.TextosDeCartelWebRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartelPresentationMapper {

    public ArmarOpcionesDeCartelRequest toArmarRequest(UsuarioId usuarioId, OpcionesDeCartelWebRequest request) {
        return new ArmarOpcionesDeCartelRequest(
                usuarioId,
                request.tipo(),
                request.formato(),
                request.titular(),
                request.bajada(),
                request.protagonista(),
                request.detalle(),
                request.lineas() == null ? List.of() : request.lineas());
    }

    public OpcionesDeCartelResponse toResponse(ArmarOpcionesDeCartelResponse response) {
        return new OpcionesDeCartelResponse(response.opciones().stream()
                .map(opcion -> new OpcionesDeCartelResponse.OpcionResponse(opcion.variante(), opcion.dibujo()))
                .toList());
    }

    public RedactarTextosDeCartelRequest toRedactarRequest(UsuarioId usuarioId, TextosDeCartelWebRequest request) {
        return new RedactarTextosDeCartelRequest(
                usuarioId, request.tiposPosibles(), request.productos(), request.numeros());
    }

    public TextosDeCartelResponse toResponse(TextosDeCartelItem textos) {
        return new TextosDeCartelResponse(
                textos.plantilla(), textos.titular(), textos.bajada(), textos.textoPosteo(), textos.redactadoConIa());
    }
}
