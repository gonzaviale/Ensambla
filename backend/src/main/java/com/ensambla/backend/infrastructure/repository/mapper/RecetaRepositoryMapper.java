package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.RecetaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RecetaRepositoryMapper {

    public Receta toDomain(RecetaEntity entity) {
        return Receta.builder()
                .id(Identificador.opcional(entity.getId(), RecetaId::de))
                .negocioId(Identificador.opcional(entity.getNegocioId(), NegocioId::de))
                .productoId(Identificador.opcional(entity.getProductoId(), RegistroId::de))
                .campoPrecioId(Identificador.opcional(entity.getCampoPrecioId(), CampoId::de))
                .lineas(entity.getLineas().stream().map(this::toLinea).toList())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public RecetaEntity toEntity(Receta receta) {
        return RecetaEntity.builder()
                .id(Identificador.valorDe(receta.getId()))
                .negocioId(Identificador.valorDe(receta.getNegocioId()))
                .productoId(Identificador.valorDe(receta.getProductoId()))
                .campoPrecioId(Identificador.valorDe(receta.getCampoPrecioId()))
                .lineas(receta.getLineas().stream().map(this::toJson).toList())
                .creadoEn(receta.getCreadoEn())
                .actualizadoEn(receta.getActualizadoEn())
                .build();
    }

    private LineaReceta toLinea(Map<String, Object> json) {
        return new LineaReceta(
                RegistroId.de((String) json.get("insumoId")),
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
