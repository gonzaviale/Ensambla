package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacionId;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsappId;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsappId;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsappId;
import com.ensambla.backend.infrastructure.repository.entity.CodigoVinculacionEntity;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.VerificacionWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import org.springframework.stereotype.Component;

@Component
public class WhatsappRepositoryMapper {

    public VinculoWhatsapp toDomain(VinculoWhatsappEntity entity) {
        return VinculoWhatsapp.builder()
                .id(Identificador.opcional(entity.getId(), VinculoWhatsappId::de))
                .usuarioId(UsuarioId.de(entity.getUsuarioId()))
                .negocioId(NegocioId.de(entity.getNegocioId()))
                .telefono(entity.getTelefono())
                .vinculadoEn(entity.getVinculadoEn())
                .build();
    }

    public VinculoWhatsappEntity toEntity(VinculoWhatsapp vinculo) {
        return VinculoWhatsappEntity.builder()
                .id(Identificador.valorDe(vinculo.getId()))
                .usuarioId(vinculo.getUsuarioId().valor())
                .negocioId(vinculo.getNegocioId().valor())
                .telefono(vinculo.getTelefono())
                .vinculadoEn(vinculo.getVinculadoEn())
                .build();
    }

    public CodigoVinculacion toDomain(CodigoVinculacionEntity entity) {
        return CodigoVinculacion.builder()
                .id(Identificador.opcional(entity.getId(), CodigoVinculacionId::de))
                .usuarioId(UsuarioId.de(entity.getUsuarioId()))
                .codigo(entity.getCodigo())
                .expiraEn(entity.getExpiraEn())
                .usadoEn(entity.getUsadoEn())
                .build();
    }

    public CodigoVinculacionEntity toEntity(CodigoVinculacion codigo) {
        return CodigoVinculacionEntity.builder()
                .id(Identificador.valorDe(codigo.getId()))
                .usuarioId(codigo.getUsuarioId().valor())
                .codigo(codigo.getCodigo())
                .expiraEn(codigo.getExpiraEn())
                .usadoEn(codigo.getUsadoEn())
                .build();
    }

    public MensajeWhatsapp toDomain(MensajeWhatsappEntity entity) {
        return MensajeWhatsapp.builder()
                .id(Identificador.opcional(entity.getId(), MensajeWhatsappId::de))
                .negocioId(NegocioId.de(entity.getNegocioId()))
                .telefono(entity.getTelefono())
                .idExterno(entity.getIdExterno())
                .tipo(entity.getTipo())
                .texto(entity.getTexto())
                .mimeMedia(entity.getMimeMedia())
                .duracionSeg(entity.getDuracionSeg())
                .estado(entity.getEstado())
                .recibidoEn(entity.getRecibidoEn())
                .intentos(entity.getIntentos())
                .reclamadoHasta(entity.getReclamadoHasta())
                .atendidoEn(entity.getAtendidoEn())
                .error(entity.getError())
                .build();
    }

    public MensajeWhatsappEntity toEntity(MensajeWhatsapp mensaje) {
        return MensajeWhatsappEntity.builder()
                .id(Identificador.valorDe(mensaje.getId()))
                .negocioId(mensaje.getNegocioId().valor())
                .telefono(mensaje.getTelefono())
                .idExterno(mensaje.getIdExterno())
                .tipo(mensaje.getTipo())
                .texto(mensaje.getTexto())
                .mimeMedia(mensaje.getMimeMedia())
                .duracionSeg(mensaje.getDuracionSeg())
                .estado(mensaje.getEstado())
                .recibidoEn(mensaje.getRecibidoEn())
                .intentos(mensaje.getIntentos())
                .reclamadoHasta(mensaje.getReclamadoHasta())
                .atendidoEn(mensaje.getAtendidoEn())
                .error(mensaje.getError())
                .build();
    }

    public VerificacionWhatsapp toDomain(VerificacionWhatsappEntity entity) {
        return VerificacionWhatsapp.builder()
                .id(Identificador.opcional(entity.getId(), VerificacionWhatsappId::de))
                .usuarioId(UsuarioId.de(entity.getUsuarioId()))
                .telefono(entity.getTelefono())
                .codigoHash(entity.getCodigoHash())
                .intentos(entity.getIntentos())
                .enviadoEn(entity.getEnviadoEn())
                .expiraEn(entity.getExpiraEn())
                .enviosVentana(entity.getEnviosVentana())
                .ventanaDesde(entity.getVentanaDesde())
                .build();
    }

    public VerificacionWhatsappEntity toEntity(VerificacionWhatsapp verificacion) {
        return VerificacionWhatsappEntity.builder()
                .id(Identificador.valorDe(verificacion.getId()))
                .usuarioId(verificacion.getUsuarioId().valor())
                .telefono(verificacion.getTelefono())
                .codigoHash(verificacion.getCodigoHash())
                .intentos(verificacion.getIntentos())
                .enviadoEn(verificacion.getEnviadoEn())
                .expiraEn(verificacion.getExpiraEn())
                .enviosVentana(verificacion.getEnviosVentana())
                .ventanaDesde(verificacion.getVentanaDesde())
                .build();
    }
}
