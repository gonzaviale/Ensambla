package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.infrastructure.repository.entity.RecetaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RecetaRepositoryMapper {

    public Receta toDomain(RecetaEntity entity) {
        return Receta.builder()
                .id(entity.getId())
                .negocioId(entity.getNegocioId())
                .productoId(entity.getProductoId())
                .campoPrecioId(entity.getCampoPrecioId())
                .lineas(entity.getLineas().stream().map(this::toLinea).toList())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public RecetaEntity toEntity(Receta receta) {
        return RecetaEntity.builder()
                .id(receta.getId())
                .negocioId(receta.getNegocioId())
                .productoId(receta.getProductoId())
                .campoPrecioId(receta.getCampoPrecioId())
                .lineas(receta.getLineas().stream().map(this::toJson).toList())
                .creadoEn(receta.getCreadoEn())
                .actualizadoEn(receta.getActualizadoEn())
                .build();
    }

    private LineaReceta toLinea(Map<String, Object> json) {
        return new LineaReceta(
                UUID.fromString((String) json.get("insumoId")),
                new BigDecimal((String) json.get("cantidad")),
                (String) json.get("unidad"));
    }

    private Map<String, Object> toJson(LineaReceta linea) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("insumoId", linea.insumoId().toString());
        json.put("cantidad", linea.cantidad().toPlainString());
        json.put("unidad", linea.unidad());
        return json;
    }
}
