package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import org.springframework.stereotype.Component;

@Component
public class BloqueRepositoryMapper {

    public Bloque toDomain(BloqueEntity entity) {
        return Bloque.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .proyectoId(entity.getProyectoId())
                .seccionId(entity.getSeccionId())
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
                .id(bloque.getId())
                .negocioId(bloque.getNegocioId())
                .proyectoId(bloque.getProyectoId())
                .seccionId(bloque.getSeccionId())
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
