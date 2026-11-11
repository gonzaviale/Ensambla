package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ConversacionCliente;
import com.ensambla.backend.domain.model.pedido.LineaDeCompra;
import com.ensambla.backend.domain.model.pedido.ConversacionClienteId;
import com.ensambla.backend.infrastructure.repository.entity.ConversacionClienteEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Los comercios entre los que el cliente tiene que elegir y su lista de compra se guardan como JSON. */
@Component
@RequiredArgsConstructor
public class ConversacionClienteRepositoryMapper {

    private final ObjectMapper objectMapper;

    public ConversacionCliente toDomain(ConversacionClienteEntity entity) {
        return ConversacionCliente.builder()
                .id(Identificador.opcional(entity.getId(), ConversacionClienteId::de))
                .clienteId(ClienteId.de(entity.getClienteId()))
                .estado(entity.getEstado())
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .opciones(objectMapper.readValue(entity.getOpciones(), new TypeReference<List<NegocioId>>() { }))
                .lista(objectMapper.readValue(entity.getLista(), new TypeReference<List<LineaDeCompra>>() { }))
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public ConversacionClienteEntity toEntity(ConversacionCliente conversacion) {
        return ConversacionClienteEntity.builder()
                .id(Identificador.valorDe(conversacion.getId()))
                .clienteId(conversacion.getClienteId().valor())
                .estado(conversacion.getEstado())
                .negocioId(Identificador.valorDe(conversacion.getNegocioId()))
                .opciones(objectMapper.writeValueAsString(conversacion.getOpciones()))
                .lista(objectMapper.writeValueAsString(conversacion.getLista()))
                .actualizadoEn(conversacion.getActualizadoEn())
                .build();
    }
}
