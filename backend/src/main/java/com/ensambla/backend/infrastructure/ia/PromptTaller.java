package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.CampoComponente;
import com.ensambla.backend.domain.model.sami.taller.CampoNuevo;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.CatalogoComponentes;
import com.ensambla.backend.domain.model.sami.taller.CatalogoTaller;
import com.ensambla.backend.domain.model.sami.taller.ComponenteCatalogo;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.KitRubro;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.PiezaKit;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RegistroTaller;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Arma el pedido a la IA para un turno del taller. Las instrucciones fijas son {@code ia/prompt-taller.md} más una
 * línea con los componentes del editor generada del catálogo (así nunca queda vieja). El contexto es corto: el sistema
 * actual (cada pieza con las etiquetas de sus datos y sus registros reales con su id), el kit del rubro reconocido (o,
 * si no hay, la lista de kits), la charla reciente y el mensaje. Sin filas de ejemplo, para que el turno cueste pocos
 * tokens (meta: menos de 2000 de entrada contando las herramientas, más lo que pesen los registros). Si cambia el
 * prompt o las herramientas, subir {@link #VERSION} y volver a medir el acierto.
 */
public class PromptTaller {

    /** v5: los registros cargados van con su id y se corrigen con {@code editar_registros}. */
    public static final String VERSION = "v5";
    static final String MARCA_MENSAJE = "Mensaje de la persona:";
    static final String CON_FOTO = "La persona adjuntó una foto. En ESTE turno hacé todo esto: (1) en 1 o 2 frases"
            + " contale lo que ENTENDISTE de la foto («Veo un cuaderno de pedidos por clienta: Ana, Vero y Giz, con lo"
            + " que pidió cada una y un total»); (2) armá una primera versión SIN esperar respuestas: usar_kit o"
            + " agregar_pieza si el sistema está vacío, y SIEMPRE cargar_registros con TODAS las filas que se leen, sin saltear ningún renglón (cada compra de un"
            + " cliente es una fila de Ventas; contá los renglones y no dejes ninguno afuera; uno tachado no se carga y"
            + " lo mencionás). En cada fila de Ventas separá los datos: Cliente (la clienta del bloque), Producto SIN la"
            + " cantidad en el nombre («Almohaditas Frutilla», no «Almohaditas Frutilla 1/2»), Cantidad como número (1/2 = 0.5,"
            + " 1/4 = 0.25, «1» = 1) y Precio; y también con los productos DISTINTOS que aparecen (nombre y precio de"
            + " lista si se ve) en la pieza de productos/inventario, sin inventar costos ni cantidades de stock;"
            + " (3) al FINAL, como última herramienta y SIEMPRE (un turno con foto sin ofrecer_opciones está incompleto), invocá"
            + " ofrecer_opciones UNA sola vez con TODAS tus dudas de"
            + " lo ambiguo en `preguntas` (hasta 3; por ejemplo qué significan los tildes, si el número de la derecha"
            + " es costo o lo que deben, cuál de dos totales es el correcto). Nunca contestes"
            + " sólo con la pregunta ni digas «antes de arrancar»."
            + " Los pedidos o ventas de clientes van en Ventas (o Pedidos) con su Cliente, nunca en"
            + " Compras ni con la clienta como proveedor; si hay deuda, es lo que cada cliente debe de esos pedidos"
            + " (Fiado = Sí / Debe), no una lista aparte inventada. Armá las piezas con las columnas de la foto"
            + " («Vela» va en «Producto», «Clienta» en «Cliente») o renombrá antes el dato con editar_campo. Lo que"
            + " viene de la foto son datos reales: no cargues ejemplos (ni uses los del kit) y no inventes ninguna fila, producto ni cliente lo que no se"
            + " lee ni lo que no está en la foto. Mientras la persona no confirme, hablá de «primera versión» y de lo"
            + " que supusiste, nunca de «tus datos ya quedaron». Si en la conversación reciente ya respondió tus"
            + " dudas, ajustá con las herramientas (editar_campo, editar_registros…) según sus respuestas; los"
            + " registros que ya cargaste se corrigen con editar_registros por su id, nunca cargándolos de nuevo.";
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
        String registros = pieza.registros().stream()
                .map(this::registro)
                .collect(Collectors.joining());
        return "- %stipo %s, «%s»%s: %s\n%s".formatted(id, pieza.tipo(), pieza.nombre(), seccion, campos, registros);
    }

    /** Un registro ya cargado, con su id para corregirlo o borrarlo: {@code   · r1: Producto=Vela, Precio=3000}. */
    private String registro(RegistroTaller registro) {
        String datos = registro.datos().entrySet().stream()
                .map(dato -> dato.getKey() + "=" + dato.getValue())
                .collect(Collectors.joining(", "));
        return "  · %s: %s\n".formatted(registro.id(), datos);
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
