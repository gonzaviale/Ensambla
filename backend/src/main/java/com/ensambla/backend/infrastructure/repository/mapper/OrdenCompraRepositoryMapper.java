package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ItemDeOrden;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.infrastructure.repository.entity.OrdenCompraEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Los ítems de la orden se guardan como JSON. */
@Component
@RequiredArgsConstructor
public class OrdenCompraRepositoryMapper {

    private final ObjectMapper objectMapper;

    public OrdenCompra toDomain(OrdenCompraEntity entity) {
        return OrdenCompra.builder()
                .id(Identificador.opcional(entity.getId(), OrdenCompraId::de))
                .negocioId(NegocioId.de(entity.getNegocioId()))
                .clienteId(ClienteId.de(entity.getClienteId()))
                .estado(entity.getEstado())
                .items(objectMapper.readValue(entity.getItems(), new TypeReference<List<ItemDeOrden>>() { }))
                .total(entity.getTotal())
                .motivoRechazo(entity.getMotivoRechazo())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public OrdenCompraEntity toEntity(OrdenCompra orden) {
        return OrdenCompraEntity.builder()
                .id(Identificador.valorDe(orden.getId()))
                .negocioId(orden.getNegocioId().valor())
                .clienteId(orden.getClienteId().valor())
                .estado(orden.getEstado())
                .items(objectMapper.writeValueAsString(orden.getItems()))
                .total(orden.getTotal())
                .motivoRechazo(orden.getMotivoRechazo())
                .creadoEn(orden.getCreadoEn())
                .actualizadoEn(orden.getActualizadoEn())
                .build();
    }
}
