package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.MensajeCliente;
import com.ensambla.backend.domain.model.pedido.MensajeClienteId;
import com.ensambla.backend.infrastructure.repository.entity.ClienteWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.MensajeClienteEntity;
import org.springframework.stereotype.Component;

@Component
public class PedidoRepositoryMapper {

    public ClienteWhatsapp toDomain(ClienteWhatsappEntity entity) {
        return ClienteWhatsapp.builder()
                .id(Identificador.opcional(entity.getId(), ClienteId::de))
                .telefono(entity.getTelefono())
                .ultimoNegocioId(Identificador.opcional(entity.getUltimoNegocioId(), NegocioId::de))
                .creadoEn(entity.getCreadoEn())
                .build();
    }

    public ClienteWhatsappEntity toEntity(ClienteWhatsapp cliente) {
        return ClienteWhatsappEntity.builder()
                .id(Identificador.valorDe(cliente.getId()))
                .telefono(cliente.getTelefono())
                .ultimoNegocioId(Identificador.valorDe(cliente.getUltimoNegocioId()))
                .creadoEn(cliente.getCreadoEn())
                .build();
    }

    public MensajeCliente toDomain(MensajeClienteEntity entity) {
        return MensajeCliente.builder()
                .id(Identificador.opcional(entity.getId(), MensajeClienteId::de))
                .telefono(entity.getTelefono())
                .idExterno(entity.getIdExterno())
                .texto(entity.getTexto())
                .estado(entity.getEstado())
                .recibidoEn(entity.getRecibidoEn())
                .intentos(entity.getIntentos())
                .reclamadoHasta(entity.getReclamadoHasta())
                .atendidoEn(entity.getAtendidoEn())
                .error(entity.getError())
                .build();
    }

    public MensajeClienteEntity toEntity(MensajeCliente mensaje) {
        return MensajeClienteEntity.builder()
                .id(Identificador.valorDe(mensaje.getId()))
                .telefono(mensaje.getTelefono())
                .idExterno(mensaje.getIdExterno())
                .texto(mensaje.getTexto())
                .estado(mensaje.getEstado())
                .recibidoEn(mensaje.getRecibidoEn())
                .intentos(mensaje.getIntentos())
                .reclamadoHasta(mensaje.getReclamadoHasta())
                .atendidoEn(mensaje.getAtendidoEn())
                .error(mensaje.getError())
                .build();
    }
}
