package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.model.bloque.AnchoBloque;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.ColorBloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.DistribucionProyecto;
import com.ensambla.backend.domain.model.proyecto.PlantillaSistema;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * El sistema con la forma en que lo arma y lo muestra la pantalla; los nombres de los datos son los del front. Sabe
 * traducirse al modelo relacional del backend y volver: la pantalla identifica las columnas por una clave y guarda las
 * filas por esa clave; la base guarda las filas por el id de la columna.
 */
public record SistemaItem(
        String id,
        String name,
        String createdAt,
        String updatedAt,
        List<SeccionItem> sections,
        String template,
        OpcionesItem options,
        String layout,
        boolean demoAlmacen) {

    public record OpcionesItem(boolean movements, boolean expenses) {
    }

    public record SeccionItem(String id, String name, List<BloqueItem> blocks) {
    }

    /** {@code rows}: cada fila trae su {@code id} y los valores por clave de columna. */
    public record BloqueItem(
            String id,
            String type,
            String name,
            String description,
            String color,
            String width,
            List<CampoItem> fields,
            List<Map<String, Object>> rows,
            String content,
            Boolean startRow,
            Integer stockDirection) {
    }

    public record CampoItem(
            String key,
            String label,
            String type,
            boolean required,
            List<String> options,
            BigDecimal min,
            String target) {
    }


    private static final int MAX_NOMBRE = 120;
    private static final int MAX_ORIGEN = 120;
    private static final String SIN_NOMBRE = "Sin nombre";
    private static final String SECCION_GENERAL = "Vista general";
    private static final String CLAVE_ID = "id";
    private static final String CLAVE_EJEMPLO = "__example";
    private static final String CLAVE_ORIGEN = "__source";
    private static final Pattern CLAVE_VALIDA = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

    private static final Map<String, TipoBloque> TIPOS_DE_BLOQUE = Map.of(
            "inventory", TipoBloque.INVENTARIO,
            "orders", TipoBloque.PEDIDOS,
            "clients", TipoBloque.CLIENTES,
            "sales", TipoBloque.VENTAS,
            "expenses", TipoBloque.GASTOS,
            "summary", TipoBloque.RESUMEN,
            "form", TipoBloque.FORMULARIO,
            "note", TipoBloque.NOTA,
            "entries", TipoBloque.ENTRADAS,
            "exits", TipoBloque.SALIDAS);

    private static final Map<String, ColorBloque> COLORES = Map.of(
            "orange", ColorBloque.NARANJA,
            "coral", ColorBloque.CORAL,
            "yellow", ColorBloque.AMARILLO,
            "blue", ColorBloque.CELESTE,
            "petrol", ColorBloque.PETROLEO);

    private static final Map<String, AnchoBloque> ANCHOS = Map.of(
            "third", AnchoBloque.TERCIO,
            "half", AnchoBloque.MEDIO,
            "wide", AnchoBloque.ANCHO,
            "full", AnchoBloque.COMPLETO);

    private static final Map<String, DistribucionProyecto> DISTRIBUCIONES = Map.of(
            "mosaic", DistribucionProyecto.MOSAICO,
            "single", DistribucionProyecto.UNA,
            "two", DistribucionProyecto.DOS,
            "three", DistribucionProyecto.TRES);

    private static final Map<String, PlantillaSistema> PLANTILLAS = Map.of(
            "shop", PlantillaSistema.ALMACEN,
            "warehouse", PlantillaSistema.DEPOSITO,
            "business", PlantillaSistema.PEDIDOS_Y_CLIENTES);

    public static SistemaItem de(Sistema sistema) {
        Proyecto proyecto = sistema.proyecto();
        return new SistemaItem(
                IdSistema.aFront(proyecto.getId()),
                proyecto.getNombre(),
                fecha(proyecto.getCreadoEn()),
                fecha(proyecto.getActualizadoEn()),
                secciones(sistema),
                plantillaDe(proyecto),
                new OpcionesItem(proyecto.getConMovimientos(), proyecto.getConGastos()),
                clave(DISTRIBUCIONES, proyecto.getDistribucion()),
                proyecto.getEsDemo());
    }

    private static String plantillaDe(Proyecto proyecto) {
        return proyecto.getPlantillaClave() == null ? null : clave(PLANTILLAS, proyecto.getPlantillaClave());
    }

    private static List<SeccionItem> secciones(Sistema sistema) {
        List<Seccion> secciones = seccionesOrdenadas(sistema);
        if (secciones.isEmpty()) {
            return List.of(seccionGeneral(sistema));
        }
        Set<SeccionId> conocidas = secciones.stream().map(Seccion::getId).collect(Collectors.toSet());
        return IntStream.range(0, secciones.size())
                .mapToObj(indice -> seccionConSusBloques(sistema, secciones.get(indice), indice == 0, conocidas))
                .toList();
    }

    private static List<Seccion> seccionesOrdenadas(Sistema sistema) {
        return sistema.secciones().stream()
                .sorted(Comparator.comparing(Seccion::getPosicion).thenComparing(seccion -> seccion.getId().valor()))
                .toList();
    }

    private static SeccionItem seccionGeneral(Sistema sistema) {
        return new SeccionItem(
                IdSistema.aFront(IdSistema.aId(sistema.id(), "general", SeccionId::de)),
                SECCION_GENERAL,
                bloques(sistema, sistema.proyecto().getBloques()));
    }

    /** Los bloques de una sección; los que apuntan a una sección que ya no existe van a la primera. */
    private static SeccionItem seccionConSusBloques(Sistema sistema, Seccion seccion, boolean primera,
                                                    Set<SeccionId> conocidas) {
        List<Bloque> suyos = sistema.proyecto().getBloques().stream()
                .filter(bloque -> seccion.getId().equals(bloque.getSeccionId())
                        || primera && !conocidas.contains(bloque.getSeccionId()))
                .toList();
        return new SeccionItem(IdSistema.aFront(seccion.getId()), seccion.getNombre(), bloques(sistema, suyos));
    }

    private static List<BloqueItem> bloques(Sistema sistema, List<Bloque> bloques) {
        return bloques.stream()
                .sorted(Comparator.comparing(Bloque::getPosicion).thenComparing(bloque -> bloque.getId().valor()))
                .map(bloque -> bloque(sistema, bloque))
                .toList();
    }

    private static BloqueItem bloque(Sistema sistema, Bloque bloque) {
        List<Campo> campos = camposOrdenados(bloque);
        List<Registro> filas = sistema.registrosPorBloque().getOrDefault(bloque.getId(), List.of());
        return new BloqueItem(
                IdSistema.aFront(bloque.getId()),
                clave(TIPOS_DE_BLOQUE, bloque.getTipo()),
                bloque.getNombre(),
                bloque.getDescripcion(),
                clave(COLORES, bloque.getColor()),
                clave(ANCHOS, bloque.getAncho()),
                campos.stream().map(SistemaItem::campo).toList(),
                filas.stream().map(fila -> fila(fila, campos)).toList(),
                bloque.getContenido(),
                bloque.getEmpiezaFila(),
                bloque.getDireccionStock() == null ? null : Integer.valueOf(bloque.getDireccionStock()));
    }

    private static List<Campo> camposOrdenados(Bloque bloque) {
        return bloque.getCampos().stream()
                .sorted(Comparator.comparing(Campo::getPosicion).thenComparing(campo -> campo.getId().valor()))
                .toList();
    }

    private static CampoItem campo(Campo campo) {
        boolean esOpcion = campo.getTipo() == TipoCampo.OPCION;
        return new CampoItem(
                campo.claveDePantalla(),
                campo.getNombre(),
                TipoDeCampoEnPantalla.claveDe(campo.getTipo()),
                campo.esObligatorio(),
                esOpcion ? campo.getOpciones() : null,
                campo.getMinimo(),
                campo.esRelacion() ? IdSistema.aFront(campo.getBloqueRelacionadoId()) : null);
    }

    private static Map<String, Object> fila(Registro registro, List<Campo> campos) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put(CLAVE_ID, IdSistema.aFront(registro.getId()));
        if (Boolean.TRUE.equals(registro.getEsEjemplo())) {
            fila.put(CLAVE_EJEMPLO, true);
        }
        if (registro.getOrigen() != null) {
            fila.put(CLAVE_ORIGEN, registro.getOrigen());
        }
        campos.stream()
                .filter(campo -> registro.getValores().containsKey(campo.getId().toString()))
                .forEach(campo -> fila.put(campo.claveDePantalla(), registro.getValores().get(campo.getId().toString())));
        return fila;
    }

    private static <T> String clave(Map<String, T> tabla, T valor) {
        return tabla.entrySet().stream()
                .filter(entrada -> entrada.getValue() == valor)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    private static String fecha(OffsetDateTime fecha) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(fecha);
    }

    /** Lo que va saliendo de recorrer el documento de la pantalla. */
    private static final class SistemaArmado {

        private final List<Seccion> secciones = new ArrayList<>();
        private final List<Bloque> bloques = new ArrayList<>();
        private final Map<BloqueId, List<Registro>> registros = new LinkedHashMap<>();
    }

    /**
     * Arma el sistema del negocio a partir del documento de la pantalla. Lo que el modelo no admite se corrige en vez
     * de rechazar el guardado (la pantalla reintenta sin parar si falla): nombres vacíos o largos, columnas de opción
     * sin opciones, conexiones a un bloque que no está, claves repetidas.
     */
    public Sistema aSistema(NegocioId negocioId, String idDelFront, OffsetDateTime ahora) {
        ProyectoId proyectoId = IdSistema.aId(negocioId, idDelFront, ProyectoId::de);
        Set<BloqueId> bloquesDelSistema = bloquesDelSistema(negocioId);
        SistemaArmado armado = new SistemaArmado();
        for (int indice = 0; indice < sections().size(); indice++) {
            armarSeccion(armado, negocioId, proyectoId, indice, bloquesDelSistema, ahora);
        }
        return new Sistema(proyecto(negocioId, proyectoId, armado.bloques, ahora), armado.secciones, armado.registros);
    }

    private Set<BloqueId> bloquesDelSistema(NegocioId negocioId) {
        return sections().stream()
                .flatMap(seccion -> seccion.blocks().stream())
                .map(bloque -> IdSistema.aId(negocioId, bloque.id(), BloqueId::de))
                .collect(Collectors.toSet());
    }

    private void armarSeccion(SistemaArmado armado, NegocioId negocioId, ProyectoId proyectoId, int indice,
                              Set<BloqueId> bloquesDelSistema, OffsetDateTime ahora) {
        SeccionItem item = sections().get(indice);
        SeccionId seccionId = IdSistema.aId(negocioId, item.id(), SeccionId::de);
        armado.secciones.add(seccion(negocioId, proyectoId, seccionId, item, indice, ahora));
        for (BloqueItem bloqueItem : item.blocks()) {
            Bloque bloque = bloque(negocioId, proyectoId, seccionId, (short) armado.bloques.size(),
                    bloqueItem, bloquesDelSistema, ahora);
            armado.bloques.add(bloque);
            armado.registros.put(bloque.getId(), filas(negocioId, bloque, bloqueItem, ahora));
        }
    }

    private static Seccion seccion(NegocioId negocioId, ProyectoId proyectoId, SeccionId seccionId, SeccionItem item,
                                   int posicion, OffsetDateTime ahora) {
        return Seccion.builder()
                .id(seccionId)
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre(nombre(item.name()))
                .posicion((short) posicion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    private Proyecto proyecto(NegocioId negocioId, ProyectoId proyectoId, List<Bloque> bloques, OffsetDateTime ahora) {
        return Proyecto.builder()
                .id(proyectoId)
                .negocioId(negocioId)
                .nombre(nombre(name()))
                .plantillaClave(template() == null ? null : PLANTILLAS.get(template()))
                .distribucion(layout() == null
                        ? DistribucionProyecto.MOSAICO
                        : DISTRIBUCIONES.getOrDefault(layout(), DistribucionProyecto.MOSAICO))
                .conMovimientos(options() != null && options().movements())
                .conGastos(options() != null && options().expenses())
                .esDemo(demoAlmacen())
                .creadoEn(creadoEn(createdAt(), ahora))
                .actualizadoEn(ahora)
                .bloques(bloques)
                .build();
    }

    private static Bloque bloque(NegocioId negocioId, ProyectoId proyectoId, SeccionId seccionId, short posicion,
                                 BloqueItem item, Set<BloqueId> bloquesDelSistema, OffsetDateTime ahora) {
        BloqueId bloqueId = IdSistema.aId(negocioId, item.id(), BloqueId::de);
        return Bloque.builder()
                .id(bloqueId)
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .seccionId(seccionId)
                .nombre(nombre(item.name()))
                .posicion(posicion)
                .tipo(TIPOS_DE_BLOQUE.getOrDefault(item.type(), TipoBloque.FORMULARIO))
                .descripcion(Objects.requireNonNullElse(item.description(), ""))
                .color(COLORES.getOrDefault(item.color(), ColorBloque.CELESTE))
                .ancho(ANCHOS.getOrDefault(item.width(), AnchoBloque.MEDIO))
                .contenido(Objects.requireNonNullElse(item.content(), ""))
                .empiezaFila(Boolean.TRUE.equals(item.startRow()))
                .direccionStock(direccionDeStock(item.stockDirection()))
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .campos(campos(negocioId, bloqueId, item.fields(), bloquesDelSistema, ahora))
                .build();
    }

    private static List<Campo> campos(NegocioId negocioId, BloqueId bloqueId, List<CampoItem> items,
                                      Set<BloqueId> bloquesDelSistema, OffsetDateTime ahora) {
        List<Campo> campos = new ArrayList<>();
        Set<String> clavesUsadas = new HashSet<>();
        for (CampoItem item : items) {
            CampoId campoId = campoId(bloqueId, item.key());
            String clave = claveDePantallaValida(item, clavesUsadas);
            campos.add(campo(negocioId, bloqueId, campoId, clave, (short) campos.size(), item,
                    bloquesDelSistema, ahora));
        }
        return campos;
    }

    /** La clave de pantalla del dato, o ninguna si no es válida o ya la usó otro dato del bloque. */
    private static String claveDePantallaValida(CampoItem item, Set<String> clavesUsadas) {
        boolean esValida = CLAVE_VALIDA.matcher(item.key()).matches() && clavesUsadas.add(item.key());
        return esValida ? item.key() : null;
    }

    private static Campo campo(NegocioId negocioId, BloqueId bloqueId, CampoId campoId, String clave, short posicion,
                               CampoItem item, Set<BloqueId> bloquesDelSistema, OffsetDateTime ahora) {
        BloqueId destino = item.target() == null ? null : IdSistema.aId(negocioId, item.target(), BloqueId::de);
        List<String> opciones = item.options() == null ? List.of() : item.options();
        TipoCampo tipo = tipoQueSePuedeGuardar(
                TipoDeCampoEnPantalla.tipoAlGuardar(item.type()), destino, opciones, bloquesDelSistema);
        return Campo.builder()
                .id(campoId)
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .nombre(nombre(item.label()))
                .tipo(tipo)
                .obligatorio(item.required())
                .opciones(tipo == TipoCampo.OPCION ? opciones : List.of())
                .bloqueRelacionadoId(tipo == TipoCampo.RELACION ? destino : null)
                .clave(clave)
                .minimo(item.min())
                .posicion(posicion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** Una conexión a un bloque que no está, o una lista sin opciones, se guarda como texto en vez de rechazarse. */
    private static TipoCampo tipoQueSePuedeGuardar(TipoCampo pedido, BloqueId destino, List<String> opciones,
                                                   Set<BloqueId> bloquesDelSistema) {
        boolean conexionSinDestino = pedido == TipoCampo.RELACION && !bloquesDelSistema.contains(destino);
        boolean listaSinOpciones = pedido == TipoCampo.OPCION && opciones.isEmpty();
        return conexionSinDestino || listaSinOpciones ? TipoCampo.TEXTO : pedido;
    }

    private static List<Registro> filas(NegocioId negocioId, Bloque bloque, BloqueItem item, OffsetDateTime ahora) {
        Map<String, Campo> porClave = camposPorClave(bloque, item);
        List<Registro> registros = new ArrayList<>();
        for (int orden = 0; orden < item.rows().size(); orden++) {
            registros.add(registro(negocioId, bloque.getId(), orden, item.rows().get(orden), porClave, ahora));
        }
        return registros;
    }

    private static Map<String, Campo> camposPorClave(Bloque bloque, BloqueItem item) {
        Map<String, Campo> porClave = new LinkedHashMap<>();
        for (int i = 0; i < item.fields().size(); i++) {
            porClave.putIfAbsent(item.fields().get(i).key(), bloque.getCampos().get(i));
        }
        return porClave;
    }

    private static Registro registro(NegocioId negocioId, BloqueId bloqueId, int orden, Map<String, Object> fila,
                                     Map<String, Campo> porClave, OffsetDateTime ahora) {
        Object id = fila.get(CLAVE_ID);
        return Registro.builder()
                .id(id == null ? RegistroId.nuevo() : IdSistema.aId(negocioId, id.toString(), RegistroId::de))
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .valores(valoresDeLaFila(negocioId, fila, porClave))
                .esEjemplo(Boolean.TRUE.equals(fila.get(CLAVE_EJEMPLO)))
                .origen(fila.get(CLAVE_ORIGEN) instanceof String origen ? recortar(origen, MAX_ORIGEN) : null)
                .orden(orden)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** La base guarda los valores por el id de la columna; la pantalla los manda por su clave. */
    private static Map<String, Object> valoresDeLaFila(NegocioId negocioId, Map<String, Object> fila,
                                                       Map<String, Campo> porClave) {
        Map<String, Object> valores = new LinkedHashMap<>();
        porClave.forEach((clave, campo) -> {
            if (fila.containsKey(clave)) {
                valores.put(campo.getId().toString(), valorDeColumna(negocioId, campo, fila.get(clave)));
            }
        });
        return valores;
    }

    /** Una conexión guarda el id de la fila a la que apunta, igual que las filas del backend. */
    private static Object valorDeColumna(NegocioId negocioId, Campo campo, Object valor) {
        if (campo.esRelacion() && valor instanceof String fila && !fila.isBlank()) {
            return IdSistema.aId(negocioId, fila, RegistroId::de).toString();
        }
        return valor;
    }

    private static CampoId campoId(BloqueId bloqueId, String clave) {
        return IdSistema.aId(bloqueId, clave, CampoId::de);
    }

    private static Short direccionDeStock(Integer direccion) {
        return direccion != null && (direccion == 1 || direccion == -1) ? Short.valueOf(direccion.shortValue()) : null;
    }

    private static String nombre(String nombre) {
        return nombre == null || nombre.isBlank() ? SIN_NOMBRE : recortar(nombre.strip(), MAX_NOMBRE);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private static OffsetDateTime creadoEn(String fecha, OffsetDateTime ahora) {
        if (fecha == null) {
            return ahora;
        }
        try {
            return OffsetDateTime.parse(fecha);
        } catch (DateTimeParseException e) {
            return ahora;
        }
    }
}
