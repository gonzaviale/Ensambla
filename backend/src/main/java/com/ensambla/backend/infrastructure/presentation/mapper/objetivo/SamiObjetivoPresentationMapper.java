package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.MensajeObjetivo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSamiRequest;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSamiRequest.MiniElegido;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiRequest;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiResponse;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiResponse.DatoItem;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiResponse.MiniPropuestoItem;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.GuardarObjetivoConSamiWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MiniElegidoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PropuestaObjetivoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PropuestaObjetivoWebResponse.DatoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PropuestaObjetivoWebResponse.MiniPropuestoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ProponerObjetivoWebRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SamiObjetivoPresentationMapper {

    private final ObjetivoPresentationMapper objetivoPresentationMapper;

    public ProponerObjetivoConSamiRequest toProponerRequest(UsuarioId usuarioId, ProponerObjetivoWebRequest request) {
        List<MensajeObjetivo> mensajes = request.mensajes().stream()
                .map(mensaje -> new MensajeObjetivo(mensaje.rol(), mensaje.texto()))
                .toList();
        return new ProponerObjetivoConSamiRequest(usuarioId, mensajes);
    }

    public GuardarObjetivoConSamiRequest toGuardarRequest(
            UsuarioId usuarioId, GuardarObjetivoConSamiWebRequest request) {
        List<MiniElegidoWebRequest> minis = request.minis() == null ? List.of() : request.minis();
        return new GuardarObjetivoConSamiRequest(
                usuarioId,
                request.texto(),
                Boolean.TRUE.equals(request.seguimientoManual()),
                minis.stream().map(this::toMiniElegido).toList());
    }

    public PropuestaObjetivoWebResponse toResponse(ProponerObjetivoConSamiResponse response) {
        return new PropuestaObjetivoWebResponse(
                response.respuesta(),
                response.entendido(),
                response.tipo(),
                response.esMonetario(),
                response.minis().stream().map(this::toResponse).toList(),
                response.datos().stream().map(this::toResponse).toList(),
                response.origen(),
                response.mensajesRestantes(),
                response.puedeGuardar());
    }

    public ObjetivoResponse toResponse(ObjetivoItem objetivo) {
        return objetivoPresentationMapper.toResponse(objetivo);
    }

    private MiniElegido toMiniElegido(MiniElegidoWebRequest mini) {
        return new MiniElegido(
                mini.modo(), mini.tipo(), mini.registroId(), mini.referencia(), mini.titulo(), mini.justificacion());
    }

    private MiniPropuestoWebResponse toResponse(MiniPropuestoItem mini) {
        return new MiniPropuestoWebResponse(
                mini.modo(),
                mini.tipo(),
                mini.registroId(),
                mini.referencia(),
                mini.titulo(),
                mini.justificacion(),
                mini.valorInicial(),
                mini.meta(),
                mini.aporteEstimado(),
                mini.textoConIa());
    }

    private DatoWebResponse toResponse(DatoItem dato) {
        return new DatoWebResponse(dato.fuente(), dato.hay());
    }
}
