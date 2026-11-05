package com.ensambla.backend.infrastructure.presentation.mapper.preferencia;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.preferencia.PreferenciasItem;
import com.ensambla.backend.domain.useCase.preferencia.configurarAvisosPorWhatsapp.ConfigurarAvisosPorWhatsappRequest;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferenciasRequest;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferenciasRequest;
import com.ensambla.backend.infrastructure.presentation.dto.preferencia.AvisosPorWhatsappWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.preferencia.PreferenciasResponse;
import com.ensambla.backend.infrastructure.presentation.dto.preferencia.PreferenciasWebRequest;
import org.springframework.stereotype.Component;

@Component
public class PreferenciaPresentationMapper {

    public ObtenerPreferenciasRequest toObtenerRequest(UsuarioId usuarioId) {
        return new ObtenerPreferenciasRequest(usuarioId);
    }

    public GuardarPreferenciasRequest toGuardarRequest(UsuarioId usuarioId, PreferenciasWebRequest request) {
        return new GuardarPreferenciasRequest(
                usuarioId,
                request.descartadas(),
                request.grupos(),
                request.bienvenidaVista(),
                request.nombre());
    }

    public PreferenciasResponse toResponse(PreferenciasItem preferencias) {
        return new PreferenciasResponse(
                preferencias.descartadas(),
                preferencias.grupos(),
                preferencias.bienvenidaVista(),
                preferencias.nombre(),
                preferencias.avisosPorWhatsapp());
    }

    public ConfigurarAvisosPorWhatsappRequest toConfigurarAvisosRequest(
            UsuarioId usuarioId, AvisosPorWhatsappWebRequest request) {
        return new ConfigurarAvisosPorWhatsappRequest(usuarioId, request.activo());
    }
}
