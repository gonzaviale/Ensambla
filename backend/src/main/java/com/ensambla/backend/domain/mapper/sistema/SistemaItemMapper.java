package com.ensambla.backend.domain.mapper.sistema;

import com.ensambla.backend.domain.dto.sistema.SistemaItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.bloque.AnchoBloque;
import com.ensambla.backend.domain.model.bloque.ColorBloque;
import com.ensambla.backend.domain.model.proyecto.DistribucionProyecto;
import com.ensambla.backend.domain.model.proyecto.PlantillaSistema;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;

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
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Traduce entre el sistema que arma la pantalla (un documento con secciones, bloques, columnas y filas) y el modelo
 * relacional del backend. La pantalla identifica las columnas por una clave y guarda las filas por esa clave; la base
 * guarda las filas por el id de la columna.
 */
public class SistemaItemMapper {

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

    private static final Map<String, TipoCampo> TIPOS_DE_CAMPO = Map.ofEntries(
            Map.entry("text", TipoCampo.TEXTO),
            Map.entry("textarea", TipoCampo.TEXTO_LARGO),
            Map.entry("number", TipoCampo.NUMERO),
            Map.entry("currency", TipoCampo.MONEDA),
            Map.entry("date", TipoCampo.FECHA),
            Map.entry("select", TipoCampo.OPCION),
            Map.entry("email", TipoCampo.EMAIL),
            Map.entry("tel", TipoCampo.TELEFONO),
            Map.entry("checkbox", TipoCampo.SI_NO),
            Map.entry("relation", TipoCampo.RELACION),
            Map.entry("time", TipoCampo.HORA));

    // ===== Backend -> pantalla =====

    public List<SistemaItem> toResponse(List<Sistema> sistemas) {
        return sistemas.stream().map(this::toResponse).toList();
    }

    public SistemaItem toResponse(Sistema sistema) {
        Proyecto proyecto = sistema.proyecto();
        return new SistemaItem(
                IdSistema.aFront(proyecto.getId()),
                proyecto.getNombre(),
                fecha(proyecto.getCreadoEn()),
                fecha(proyecto.getActualizadoEn()),
                secciones(sistema),
                proyecto.getPlantillaClave() == null ? null : clave(PLANTILLAS, proyecto.getPlantillaClave()),
                new OpcionesItem(proyecto.getConMovimientos(), proyecto.getConGastos()),
                clave(DISTRIBUCIONES, proyecto.getDistribucion()),
                proyecto.getEsDemo());
    }

    private List<SeccionItem> secciones(Sistema sistema) {
        List<Seccion> secciones = sistema.secciones().stream()
                .sorted(Comparator.comparing(Seccion::getPosicion).thenComparing(Seccion::getId))
                .toList();
        if (secciones.isEmpty()) {
            return List.of(new SeccionItem(
                    IdSistema.aFront(IdSistema.aUuid(sistema.id(), "general")),
                    SECCION_GENERAL,
                    bloques(sistema, sistema.proyecto().getBloques())));
        }
        Set<UUID> conocidas = secciones.stream().map(Seccion::getId).collect(Collectors.toSet());
        List<SeccionItem> items = new ArrayList<>();
        for (int i = 0; i < secciones.size(); i++) {
            Seccion seccion = secciones.get(i);
            boolean primera = i == 0;
            List<Bloque> suyos = sistema.proyecto().getBloques().stream()
                    .filter(bloque -> seccion.getId().equals(bloque.getSeccionId())
                            || primera && !conocidas.contains(bloque.getSeccionId()))
                    .toList();
            items.add(new SeccionItem(IdSistema.aFront(seccion.getId()), seccion.getNombre(), bloques(sistema, suyos)));
        }
        return items;
    }

    private List<BloqueItem> bloques(Sistema sistema, List<Bloque> bloques) {
        return bloques.stream()
                .sorted(Comparator.comparing(Bloque::getPosicion).thenComparing(Bloque::getId))
                .map(bloque -> bloque(sistema, bloque))
                .toList();
    }

    private BloqueItem bloque(Sistema sistema, Bloque bloque) {
        List<Campo> campos = bloque.getCampos().stream()
                .sorted(Comparator.comparing(Campo::getPosicion).thenComparing(Campo::getId))
                .toList();
        List<Registro> filas = sistema.registrosPorBloque().getOrDefault(bloque.getId(), List.of());
        return new BloqueItem(
                IdSistema.aFront(bloque.getId()),
                clave(TIPOS_DE_BLOQUE, bloque.getTipo()),
                bloque.getNombre(),
                bloque.getDescripcion(),
                clave(COLORES, bloque.getColor()),
                clave(ANCHOS, bloque.getAncho()),
                campos.stream().map(this::campo).toList(),
                filas.stream().map(fila -> fila(fila, campos)).toList(),
                bloque.getContenido(),
                bloque.getEmpiezaFila(),
                bloque.getDireccionStock() == null ? null : Integer.valueOf(bloque.getDireccionStock()));
    }

    private CampoItem campo(Campo campo) {
        boolean esOpcion = campo.getTipo() == TipoCampo.OPCION;
        return new CampoItem(
                claveDe(campo),
                campo.getNombre(),
                tipoDeCampo(campo.getTipo()),
                campo.esObligatorio(),
                esOpcion ? campo.getOpciones() : null,
                campo.getMinimo(),
                campo.esRelacion() ? IdSistema.aFront(campo.getBloqueRelacionadoId()) : null);
    }

    private Map<String, Object> fila(Registro registro, List<Campo> campos) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put(CLAVE_ID, IdSistema.aFront(registro.getId()));
        if (Boolean.TRUE.equals(registro.getEsEjemplo())) {
            fila.put(CLAVE_EJEMPLO, true);
        }
        if (registro.getOrigen() != null) {
            fila.put(CLAVE_ORIGEN, registro.getOrigen());
        }
        for (Campo campo : campos) {
            String idDelCampo = campo.getId().toString();
            if (registro.getValores().containsKey(idDelCampo)) {
                fila.put(claveDe(campo), registro.getValores().get(idDelCampo));
            }
        }
        return fila;
    }

    /** La clave que ve la pantalla; las columnas que no la traen (creadas por Sami o por una plantilla) usan su id. */
    private static String claveDe(Campo campo) {
        return campo.getClave() == null ? campo.getId().toString() : campo.getClave();
    }

    /** La pantalla no tiene columnas calculadas: una fórmula se muestra como número. */
    private static String tipoDeCampo(TipoCampo tipo) {
        return tipo == TipoCampo.FORMULA ? "number" : clave(TIPOS_DE_CAMPO, tipo);
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

    // ===== Pantalla -> backend =====

    /**
     * Arma el sistema del negocio a partir del documento de la pantalla. Lo que el modelo no admite se corrige en vez
     * de rechazar el guardado (la pantalla reintenta sin parar si falla): nombres vacíos o largos, columnas de opción
     * sin opciones, conexiones a un bloque que no está, claves repetidas.
     */
    public Sistema toSistema(UUID negocioId, String idDelFront, SistemaItem item, OffsetDateTime ahora) {
        UUID proyectoId = IdSistema.aUuid(negocioId, idDelFront);
        Set<UUID> bloquesDelSistema = item.sections().stream()
                .flatMap(seccion -> seccion.blocks().stream())
                .map(bloque -> IdSistema.aUuid(negocioId, bloque.id()))
                .collect(Collectors.toSet());
        List<Seccion> secciones = new ArrayList<>();
        List<Bloque> bloques = new ArrayList<>();
        Map<UUID, List<Registro>> registros = new LinkedHashMap<>();
        for (int i = 0; i < item.sections().size(); i++) {
            SeccionItem seccionItem = item.sections().get(i);
            UUID seccionId = IdSistema.aUuid(negocioId, seccionItem.id());
            secciones.add(Seccion.builder()
                    .id(seccionId)
                    .negocioId(negocioId)
                    .proyectoId(proyectoId)
                    .nombre(nombre(seccionItem.name()))
                    .posicion((short) i)
                    .creadoEn(ahora)
                    .actualizadoEn(ahora)
                    .build());
            for (BloqueItem bloqueItem : seccionItem.blocks()) {
                Bloque bloque = bloque(negocioId, proyectoId, seccionId, (short) bloques.size(),
                        bloqueItem, bloquesDelSistema, ahora);
                bloques.add(bloque);
                registros.put(bloque.getId(), filas(negocioId, bloque, bloqueItem, ahora));
            }
        }
        Proyecto proyecto = Proyecto.builder()
                .id(proyectoId)
                .negocioId(negocioId)
                .nombre(nombre(item.name()))
                .plantillaClave(item.template() == null ? null : PLANTILLAS.get(item.template()))
                .distribucion(item.layout() == null
                        ? DistribucionProyecto.MOSAICO
                        : DISTRIBUCIONES.getOrDefault(item.layout(), DistribucionProyecto.MOSAICO))
                .conMovimientos(item.options() != null && item.options().movements())
                .conGastos(item.options() != null && item.options().expenses())
                .esDemo(item.demoAlmacen())
                .creadoEn(creadoEn(item.createdAt(), ahora))
                .actualizadoEn(ahora)
                .bloques(bloques)
                .build();
        return new Sistema(proyecto, secciones, registros);
    }

    private Bloque bloque(UUID negocioId, UUID proyectoId, UUID seccionId, short posicion, BloqueItem item,
                          Set<UUID> bloquesDelSistema, OffsetDateTime ahora) {
        UUID bloqueId = IdSistema.aUuid(negocioId, item.id());
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

    private List<Campo> campos(UUID negocioId, UUID bloqueId, List<CampoItem> items, Set<UUID> bloquesDelSistema,
                               OffsetDateTime ahora) {
        List<Campo> campos = new ArrayList<>();
        Set<String> clavesUsadas = new HashSet<>();
        for (CampoItem item : items) {
            UUID campoId = campoId(bloqueId, item.key());
            String clave = CLAVE_VALIDA.matcher(item.key()).matches() && clavesUsadas.add(item.key())
                    ? item.key()
                    : null;
            campos.add(campo(negocioId, bloqueId, campoId, clave, (short) campos.size(), item, bloquesDelSistema, ahora));
        }
        return campos;
    }

    private Campo campo(UUID negocioId, UUID bloqueId, UUID campoId, String clave, short posicion, CampoItem item,
                        Set<UUID> bloquesDelSistema, OffsetDateTime ahora) {
        TipoCampo tipo = TIPOS_DE_CAMPO.getOrDefault(item.type(), TipoCampo.TEXTO);
        UUID destino = item.target() == null ? null : IdSistema.aUuid(negocioId, item.target());
        List<String> opciones = item.options() == null ? List.of() : item.options();
        if (tipo == TipoCampo.RELACION && !bloquesDelSistema.contains(destino)) {
            tipo = TipoCampo.TEXTO;
        }
        if (tipo == TipoCampo.OPCION && opciones.isEmpty()) {
            tipo = TipoCampo.TEXTO;
        }
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

    private List<Registro> filas(UUID negocioId, Bloque bloque, BloqueItem item, OffsetDateTime ahora) {
        Map<String, Campo> porClave = new LinkedHashMap<>();
        for (int i = 0; i < item.fields().size(); i++) {
            porClave.putIfAbsent(item.fields().get(i).key(), bloque.getCampos().get(i));
        }
        List<Registro> registros = new ArrayList<>();
        for (int i = 0; i < item.rows().size(); i++) {
            registros.add(registro(negocioId, bloque.getId(), i, item.rows().get(i), porClave, ahora));
        }
        return registros;
    }

    private Registro registro(UUID negocioId, UUID bloqueId, int orden, Map<String, Object> fila,
                              Map<String, Campo> porClave, OffsetDateTime ahora) {
        Map<String, Object> valores = new LinkedHashMap<>();
        porClave.forEach((clave, campo) -> {
            if (fila.containsKey(clave)) {
                valores.put(campo.getId().toString(), valorDeColumna(negocioId, campo, fila.get(clave)));
            }
        });
        Object id = fila.get(CLAVE_ID);
        return Registro.builder()
                .id(id == null ? UUID.randomUUID() : IdSistema.aUuid(negocioId, id.toString()))
                .negocioId(negocioId)
                .bloqueId(bloqueId)
                .valores(valores)
                .esEjemplo(Boolean.TRUE.equals(fila.get(CLAVE_EJEMPLO)))
                .origen(fila.get(CLAVE_ORIGEN) instanceof String origen ? recortar(origen, MAX_ORIGEN) : null)
                .orden(orden)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** Una conexión guarda el id de la fila a la que apunta, igual que las filas del backend. */
    private static Object valorDeColumna(UUID negocioId, Campo campo, Object valor) {
        if (campo.esRelacion() && valor instanceof String fila && !fila.isBlank()) {
            return IdSistema.aUuid(negocioId, fila).toString();
        }
        return valor;
    }

    private static UUID campoId(UUID bloqueId, String clave) {
        return IdSistema.aUuid(bloqueId, clave);
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
