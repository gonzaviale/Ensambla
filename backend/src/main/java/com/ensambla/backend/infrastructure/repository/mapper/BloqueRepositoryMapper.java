package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import org.springframework.stereotype.Component;

@Component
public class BloqueRepositoryMapper {

    public Bloque toDomain(BloqueEntity entity) {
        return Bloque.builder()
                .id(Identificador.opcional(entity.getId(), BloqueId::de))
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .proyectoId(Identificador.opcional(entity.getProyectoId(), ProyectoId::de))
                .seccionId(Identificador.opcional(entity.getSeccionId(), SeccionId::de))
                .tipo(entity.getTipo())
                .descripcion(entity.getDescripcion())
                .color(entity.getColor())
                .ancho(entity.getAncho())
                .contenido(entity.getContenido())
                .empiezaFila(entity.getEmpiezaFila())
                .direccionStock(entity.getDireccionStock())
                .nombre(entity.getNombre())
                .posicion(entity.getPosicion())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public BloqueEntity toEntity(Bloque bloque) {
        return BloqueEntity.builder()
                .id(Identificador.valorDe(bloque.getId()))
                .negocioId(Identificador.valorDe(bloque.getNegocioId()))
                .proyectoId(Identificador.valorDe(bloque.getProyectoId()))
                .seccionId(Identificador.valorDe(bloque.getSeccionId()))
                .tipo(bloque.getTipo())
                .descripcion(bloque.getDescripcion())
                .color(bloque.getColor())
                .ancho(bloque.getAncho())
                .contenido(bloque.getContenido())
                .empiezaFila(bloque.getEmpiezaFila())
                .direccionStock(bloque.getDireccionStock())
                .nombre(bloque.getNombre())
                .posicion(bloque.getPosicion())
                .creadoEn(bloque.getCreadoEn())
                .actualizadoEn(bloque.getActualizadoEn())
                .build();
    }
}
