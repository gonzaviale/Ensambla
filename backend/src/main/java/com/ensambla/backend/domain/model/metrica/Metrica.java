package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Una métrica guardada en un cuaderno: su receta, su lugar en el panel y quién la eligió (el rubro o la persona). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metrica {

    /** Cuántas métricas guarda como máximo un cuaderno: el panel de la home tiene que seguir siendo legible. */
    public static final int MAXIMO_POR_CUADERNO = 12;

    private MetricaId id;

    private NegocioId negocioId;

    private ProyectoId proyectoId;

    private DefinicionMetrica definicion;

    private int orden;

    private AutoriaMetrica autoria;

    private OffsetDateTime creadoEn;

    /** Valida la receta antes de guardarla; el lugar lo da quien la agrega (después de las que ya hay). */
    public static Metrica nueva(NegocioId negocioId, ProyectoId proyectoId, DefinicionMetrica definicion,
                                int orden, AutoriaMetrica autoria, OffsetDateTime ahora) {
        definicion.validar();
        return Metrica.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .definicion(definicion)
                .orden(orden)
                .autoria(autoria)
                .creadoEn(ahora)
                .build();
    }

    /** Le cambia el nombre que se ve en el panel; rechaza el nombre vacío o demasiado largo. */
    public void renombrar(String nombre) {
        DefinicionMetrica renombrada = definicion.conNombre(nombre);
        renombrada.validar();
        definicion = renombrada;
    }
}
