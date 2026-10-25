package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.CampoComponente;
import com.ensambla.backend.domain.model.sami.taller.CampoNuevo;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.FilaTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.PiezaKit;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Arma el pedido a la IA para un turno del taller. Las instrucciones fijas son {@code ia/prompt-taller.md} más una
 * línea con los componentes del editor generada del catálogo (así nunca queda vieja). El contexto es corto: el sistema
 * actual (cada pieza con las etiquetas de sus datos), el kit del rubro reconocido (o, si no hay, la lista de kits), la
 * charla reciente y el mensaje. Sin filas ni datos de ejemplo, para que el turno cueste pocos tokens (meta: menos de
 * 2000 de entrada contando las herramientas). Si cambia el prompt o las herramientas, subir {@link #VERSION} y volver
 * a medir el acierto.
 */
public class PromptTaller {

    /** v5: editar_registro y quitar_registro, con las primeras filas en el contexto. v4: tras leer la foto, Sami cuenta qué leyó, nombra dudas y pregunta. v3: fotos y {@code cargar_registros}. */
    public static final String VERSION = "v5";
    static final String MARCA_MENSAJE = "Mensaje de la persona:";
    static final String CON_FOTO = "La persona adjuntó una foto. Leé sus columnas y filas: armá o ajustá las piezas con"
            + " esas columnas (si hay kit, usalo y sumá lo que falte) y SIEMPRE invocá cargar_registros con todas las"
            + " filas que se leen: son datos reales, no ejemplos. Cada fila va con las etiquetas de los datos de la pieza"
            + " («Vela» de la foto va en «Producto», «Clienta» en «Cliente») o renombrá antes el dato con editar_campo."
            + " Lo que no se lee bien, no lo inventes: contalo. Después de cargar, decí qué leíste (cuántas filas y de"
            + " qué), nombrá las dudas concretas (una letra, un monto, una fecha) y cerrá con ofrecer_opciones"
            + " preguntando si está bien, si hay algo mal, si es de otro cuaderno o si quiere sumar otra tabla.";
    static final String MARCA_KIT = "Kit del rubro:";
    static final String SIN_PIEZAS = "Todavía no tiene piezas.";

    private final String instrucciones;
    private final String kitsDisponibles;

    public PromptTaller(Resource instrucciones, CatalogoComponentes catalogo, KitsRubro kits) {
        this.instrucciones = RecursoTexto.contenido(instrucciones).strip() + "\n" + componentes(catalogo);
        this.kitsDisponibles = "Kits por rubro (usar_kit): " + kits.kits().stream()
                .map(KitRubro::id)
                .collect(Collectors.joining(", ")) + ".";
    }

    public PeticionConHerramientas peticion(ConsultaTaller consulta) {
        List<ImagenTaller> imagenes = consulta.imagen() == null ? List.of() : List.of(consulta.imagen());
        return new PeticionConHerramientas(instrucciones, contexto(consulta), imagenes);
    }

    /**
     * Los componentes van por tipo y nombre. Sólo la tabla a medida lista sus datos base, porque sus ejemplos se
     * cargan por etiqueta; de las piezas que ya existen el contexto muestra los datos, y el backend descarta las
     * etiquetas repetidas. Así el turno cuesta menos.
     */
    private static String componentes(CatalogoComponentes catalogo) {
        return "Componentes (tipo Nombre): " + catalogo.componentes().stream()
                .map(PromptTaller::componente)
                .collect(Collectors.joining("; ")) + ".";
    }

    private static String componente(ComponenteCatalogo componente) {
        String nombre = componente.tipo() + " " + componente.nombre();
        if (!componente.repetible()) {
            return nombre;
        }
        return nombre + ": " + componente.campos().stream()
                .map(CampoComponente::etiqueta)
                .collect(Collectors.joining(", "));
    }

    private String contexto(ConsultaTaller consulta) {
        StringBuilder contexto = new StringBuilder();
        contexto.append("Sistema actual: «%s» (distribución: %s)\n"
                .formatted(consulta.estado().nombre(), consulta.estado().distribucion()));
        boolean vacio = consulta.estado().piezas().isEmpty();
        if (vacio) {
            contexto.append(SIN_PIEZAS).append('\n');
        } else {
            consulta.estado().piezas().forEach(pieza -> contexto.append(pieza(pieza)));
        }
        rubro(consulta.kit(), vacio).ifPresent(rubro -> contexto.append('\n').append(rubro).append('\n'));
        if (!consulta.historial().isEmpty()) {
            contexto.append("\nConversación reciente:\n");
            consulta.historial().forEach(mensaje -> contexto.append("%s: %s\n"
                    .formatted(mensaje.quien().equals("sami") ? "Sami" : "Persona", mensaje.texto())));
        }
        if (consulta.imagen() != null) {
            contexto.append('\n').append(CON_FOTO);
        }
        return contexto.append('\n').append(MARCA_MENSAJE).append(' ').append(consulta.mensaje()).toString();
    }

    /** Los datos van por etiqueta; sólo los vínculos llevan su tipo, para saber qué ya está conectado. */
    private String pieza(PiezaTaller pieza) {
        String campos = pieza.campos().stream()
                .map(this::campo)
                .collect(Collectors.joining(", "));
        String seccion = pieza.seccion() == null ? "" : " sección «%s»".formatted(pieza.seccion());
        // Una pieza recién armada en el turno todavía no tiene id: se la nombra por su nombre.
        String id = pieza.id() == null ? "" : "id %s, ".formatted(pieza.id());
        return "- %stipo %s, «%s»%s: %s\n%s".formatted(id, pieza.tipo(), pieza.nombre(), seccion, campos, filas(pieza));
    }

    /**
     * Las primeras filas, con sus primeros datos separados por «|». El id sólo se muestra en las que repiten su dato
     * principal, para poder distinguirlas; las demás se nombran por ese dato.
     */
    private String filas(PiezaTaller pieza) {
        if (pieza.filas().isEmpty()) {
            return "";
        }
        Map<String, Long> repetidas = pieza.filas().stream()
                .collect(Collectors.groupingBy(this::principal, Collectors.counting()));
        String filas = pieza.filas().stream()
                .map(fila -> repetidas.get(principal(fila)) > 1
                        ? "%s [id %s]".formatted(String.join(" | ", fila.valores()), fila.id())
                        : String.join(" | ", fila.valores()))
                .collect(Collectors.joining("; "));
        return "  Registros: %s\n".formatted(filas);
    }

    private String principal(FilaTaller fila) {
        return fila.valores().isEmpty() ? fila.id() : fila.valores().getFirst().strip().toLowerCase(Locale.ROOT);
    }

    private String campo(CampoTaller campo) {
        return CatalogoTaller.TIPO_VINCULO.equals(campo.tipo()) ? campo.etiqueta() + " (vínculo)" : campo.etiqueta();
    }

    /**
     * Con el sistema vacío, que es cuando se arma, va el resumen del kit reconocido o, si no hay, la lista de kits.
     * Con piezas ya armadas no va nada: nombrar el kit hacía que Sami sumara sus piezas ante un pedido puntual
     * («llamalo Velas de Ana»), y así cada turno de ajuste cuesta menos.
     */
    private Optional<String> rubro(KitRubro kit, boolean vacio) {
        if (!vacio) {
            return Optional.empty();
        }
        return Optional.of(kit == null ? kitsDisponibles : kit(kit));
    }

    /** Resumen del kit: cada pieza con su tipo y sus datos extra, y las conexiones. Sin ejemplos. */
    private String kit(KitRubro kit) {
        String piezas = kit.piezas().stream().map(this::piezaDeKit).collect(Collectors.joining("; "));
        String conexiones = kit.conexiones().stream()
                .map(conexion -> conexion.desde() + "→" + conexion.hacia())
                .collect(Collectors.joining(", "));
        return "%s %s (%s). Piezas: %s.%s".formatted(MARCA_KIT, kit.id(), kit.nombre(), piezas,
                conexiones.isEmpty() ? "" : " Conexiones: " + conexiones + ".");
    }

    private String piezaDeKit(PiezaKit pieza) {
        String extras = pieza.campos().stream().map(CampoNuevo::etiqueta).collect(Collectors.joining(", "));
        return extras.isEmpty()
                ? "%s (%s)".formatted(pieza.nombre(), pieza.tipo())
                : "%s (%s +%s)".formatted(pieza.nombre(), pieza.tipo(), extras);
    }
}
