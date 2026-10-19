package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import org.springframework.stereotype.Component;

@Component
public class ProyectoRepositoryMapper {

    public Proyecto toDomain(ProyectoEntity source) {
        return Proyecto.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .nombre(source.getNombre())
                .descripcion(source.getDescripcion())
                .plantillaId(source.getPlantillaId())
                .plantillaClave(source.getPlantillaClave())
                .distribucion(source.getDistribucion())
                .conMovimientos(source.getConMovimientos())
                .conGastos(source.getConGastos())
                .esDemo(source.getEsDemo())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public ProyectoEntity toEntity(Proyecto source) {
        return ProyectoEntity.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .nombre(source.getNombre())
                .descripcion(source.getDescripcion())
                .plantillaId(source.getPlantillaId())
                .plantillaClave(source.getPlantillaClave())
                .distribucion(source.getDistribucion())
                .conMovimientos(source.getConMovimientos())
                .conGastos(source.getConGastos())
                .esDemo(source.getEsDemo())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public Bloque toDomain(BloqueEntity source) {
        return Bloque.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .proyectoId(source.getProyectoId())
                .seccionId(source.getSeccionId())
                .tipo(source.getTipo())
                .descripcion(source.getDescripcion())
                .color(source.getColor())
                .ancho(source.getAncho())
                .contenido(source.getContenido())
                .empiezaFila(source.getEmpiezaFila())
                .direccionStock(source.getDireccionStock())
                .nombre(source.getNombre())
                .posicion(source.getPosicion())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public BloqueEntity toEntity(Bloque source) {
        return BloqueEntity.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .proyectoId(source.getProyectoId())
                .seccionId(source.getSeccionId())
                .tipo(source.getTipo())
                .descripcion(source.getDescripcion())
                .color(source.getColor())
                .ancho(source.getAncho())
                .contenido(source.getContenido())
                .empiezaFila(source.getEmpiezaFila())
                .direccionStock(source.getDireccionStock())
                .nombre(source.getNombre())
                .posicion(source.getPosicion())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public Campo toDomain(CampoEntity source) {
        return Campo.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .bloqueId(source.getBloqueId())
                .nombre(source.getNombre())
                .tipo(source.getTipo())
                .obligatorio(source.getObligatorio())
                .valorInicial(source.getValorInicial())
                .opciones(source.getOpciones())
                .bloqueRelacionadoId(source.getBloqueRelacionadoId())
                .formula(source.getFormula())
                .clave(source.getClave())
                .minimo(source.getMinimo())
                .posicion(source.getPosicion())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }

    public CampoEntity toEntity(Campo source) {
        return CampoEntity.builder()
                .id(source.getId())
                .negocioId(source.getNegocioId())
                .bloqueId(source.getBloqueId())
                .nombre(source.getNombre())
                .tipo(source.getTipo())
                .obligatorio(source.getObligatorio())
                .valorInicial(source.getValorInicial())
                .opciones(source.getOpciones())
                .bloqueRelacionadoId(source.getBloqueRelacionadoId())
                .formula(source.getFormula())
                .clave(source.getClave())
                .minimo(source.getMinimo())
                .posicion(source.getPosicion())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }
}

