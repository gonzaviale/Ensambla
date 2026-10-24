package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import org.springframework.stereotype.Component;

@Component
public class ProyectoRepositoryMapper {

    public Proyecto toDomain(ProyectoEntity source) {
        return Proyecto.builder()
                .id(Identificador.opcional(source.getId(), ProyectoId::de))
                .negocioId(Identificador.opcional(source.getNegocioId(), NegocioId::de))
                .nombre(source.getNombre())
                .descripcion(source.getDescripcion())
                .plantillaId(Identificador.opcional(source.getPlantillaId(), PlantillaId::de))
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
                .id(Identificador.valorDe(source.getId()))
                .negocioId(Identificador.valorDe(source.getNegocioId()))
                .nombre(source.getNombre())
                .descripcion(source.getDescripcion())
                .plantillaId(Identificador.valorDe(source.getPlantillaId()))
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
                .id(Identificador.opcional(source.getId(), BloqueId::de))
                .negocioId(Identificador.opcional(source.getNegocioId(), NegocioId::de))
                .proyectoId(Identificador.opcional(source.getProyectoId(), ProyectoId::de))
                .seccionId(Identificador.opcional(source.getSeccionId(), SeccionId::de))
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
                .id(Identificador.valorDe(source.getId()))
                .negocioId(Identificador.valorDe(source.getNegocioId()))
                .proyectoId(Identificador.valorDe(source.getProyectoId()))
                .seccionId(Identificador.valorDe(source.getSeccionId()))
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
                .id(Identificador.opcional(source.getId(), CampoId::de))
                .negocioId(Identificador.opcional(source.getNegocioId(), NegocioId::de))
                .bloqueId(Identificador.opcional(source.getBloqueId(), BloqueId::de))
                .nombre(source.getNombre())
                .tipo(source.getTipo())
                .obligatorio(source.getObligatorio())
                .valorInicial(source.getValorInicial())
                .opciones(source.getOpciones())
                .bloqueRelacionadoId(Identificador.opcional(source.getBloqueRelacionadoId(), BloqueId::de))
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
                .id(Identificador.valorDe(source.getId()))
                .negocioId(Identificador.valorDe(source.getNegocioId()))
                .bloqueId(Identificador.valorDe(source.getBloqueId()))
                .nombre(source.getNombre())
                .tipo(source.getTipo())
                .obligatorio(source.getObligatorio())
                .valorInicial(source.getValorInicial())
                .opciones(source.getOpciones())
                .bloqueRelacionadoId(Identificador.valorDe(source.getBloqueRelacionadoId()))
                .formula(source.getFormula())
                .clave(source.getClave())
                .minimo(source.getMinimo())
                .posicion(source.getPosicion())
                .creadoEn(source.getCreadoEn())
                .actualizadoEn(source.getActualizadoEn())
                .build();
    }
}

