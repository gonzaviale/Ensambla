package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsappId;
import com.ensambla.backend.infrastructure.repository.entity.PropuestaWhatsappEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Las filas, la lectura de la foto y las tablas ofrecidas se guardan como JSON. */
@Component
@RequiredArgsConstructor
public class PropuestaWhatsappRepositoryMapper {

    private final ObjectMapper objectMapper;

    public PropuestaWhatsapp toDomain(PropuestaWhatsappEntity entity) {
        return PropuestaWhatsapp.builder()
                .id(Identificador.opcional(entity.getId(), PropuestaWhatsappId::de))
                .negocioId(NegocioId.de(entity.getNegocioId()))
                .telefono(entity.getTelefono())
                .origen(entity.getOrigen())
                .estado(entity.getEstado())
                .bloqueId(Identificador.opcional(entity.getBloqueId(), BloqueId::de))
                .texto(entity.getTexto())
                .lectura(entity.getLectura() == null ? null
                        : objectMapper.readValue(entity.getLectura(), LecturaCuaderno.class))
                .opciones(objectMapper.readValue(entity.getOpciones(), new TypeReference<List<BloqueId>>() { }))
                .filas(objectMapper.readValue(entity.getFilas(), new TypeReference<List<FilaPropuesta>>() { }))
                .creadoEn(entity.getCreadoEn())
                .expiraEn(entity.getExpiraEn())
                .cerradoEn(entity.getCerradoEn())
                .build();
    }

    public PropuestaWhatsappEntity toEntity(PropuestaWhatsapp propuesta) {
        return PropuestaWhatsappEntity.builder()
                .id(Identificador.valorDe(propuesta.getId()))
                .negocioId(propuesta.getNegocioId().valor())
                .telefono(propuesta.getTelefono())
                .origen(propuesta.getOrigen())
                .estado(propuesta.getEstado())
                .bloqueId(Identificador.valorDe(propuesta.getBloqueId()))
                .texto(propuesta.getTexto())
                .lectura(propuesta.getLectura() == null ? null : objectMapper.writeValueAsString(propuesta.getLectura()))
                .opciones(objectMapper.writeValueAsString(propuesta.getOpciones()))
                .filas(objectMapper.writeValueAsString(propuesta.getFilas()))
                .creadoEn(propuesta.getCreadoEn())
                .expiraEn(propuesta.getExpiraEn())
                .cerradoEn(propuesta.getCerradoEn())
                .build();
    }
}
