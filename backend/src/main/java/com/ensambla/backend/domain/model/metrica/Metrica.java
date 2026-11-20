package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

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

    /** El que eligió la persona; {@code null} mientras no eligió ninguno. */
    private GraficoMetrica graficoElegido;

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

    /** Cómo se dibuja: el que eligió la persona o, si no eligió, el de siempre para su reparto. */
    public GraficoMetrica getGrafico() {
        return Objects.requireNonNullElse(graficoElegido, definicion.agrupar().graficoDeSiempre());
    }

    /** Los gráficos que se le pueden elegir: una métrica sin repartir sólo se muestra como número. */
    public List<GraficoMetrica> graficosPosibles() {
        return GraficoMetrica.posiblesPara(definicion.agrupar());
    }

    /** Elige cómo se dibuja; rechaza el gráfico que no sirve para su reparto (una dona de un solo número). */
    public void cambiarGrafico(GraficoMetrica grafico) {
        if (!graficosPosibles().contains(grafico)) {
            throw new BusinessRuleException("Ese gráfico no sirve para esta métrica: elegí otro");
        }
        graficoElegido = grafico;
    }

    /** Le cambia el nombre que se ve en el panel; rechaza el nombre vacío o demasiado largo. */
    public void renombrar(String nombre) {
        DefinicionMetrica renombrada = definicion.conNombre(nombre);
        renombrada.validar();
        definicion = renombrada;
    }
}
