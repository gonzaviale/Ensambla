package com.ensambla.backend.infrastructure.avisos;

import com.ensambla.backend.domain.model.aviso.AccionAbrirRegistro;
import com.ensambla.backend.domain.model.aviso.AccionCrearRegistro;
import com.ensambla.backend.domain.model.aviso.AccionDeAviso;
import com.ensambla.backend.domain.model.aviso.AccionDesconocida;
import com.ensambla.backend.domain.model.aviso.AccionEditarRegistro;
import com.ensambla.backend.domain.model.aviso.AccionInvalida;
import com.ensambla.backend.domain.model.aviso.AccionRegistrar;
import com.ensambla.backend.domain.model.aviso.AccionVerZona;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Traduce las acciones de un aviso entre el JSON con el que se guardan y se envían (un mapa con la clave
 * {@code tipo}) y los objetos del dominio. Es el único lugar que conoce ese formato.
 */
public final class AccionDeAvisoJson {

    private static final String SIN_REGISTRO = "La acción del aviso no indica qué registro cambiar";
    private static final String SIN_TABLA = "La acción del aviso no indica en qué tabla crear la fila";
    private static final String SIN_VALORES = "La acción del aviso no indica qué valores cambiar";

    private static final Map<String, Function<Map<String, Object>, AccionDeAviso>> LECTORES = Map.of(
            "ABRIR", AccionDeAvisoJson::leerAbrir,
            "REGISTRAR", AccionDeAvisoJson::leerRegistrar,
            "VER_ZONA", AccionDeAvisoJson::leerVerZona,
            "EDITAR_REGISTRO", AccionDeAvisoJson::leerEditar,
            "CREAR_REGISTRO", AccionDeAvisoJson::leerCrear);

    private static final Map<Class<?>, Function<AccionDeAviso, Map<String, Object>>> ESCRITORES = Map.of(
            AccionAbrirRegistro.class, accion -> escribirAbrir((AccionAbrirRegistro) accion),
            AccionRegistrar.class, accion -> escribirRegistrar((AccionRegistrar) accion),
            AccionVerZona.class, accion -> escribirVerZona((AccionVerZona) accion),
            AccionEditarRegistro.class, accion -> escribirEditar((AccionEditarRegistro) accion),
            AccionCrearRegistro.class, accion -> escribirCrear((AccionCrearRegistro) accion),
            AccionInvalida.class, accion -> ((AccionInvalida) accion).datos(),
            AccionDesconocida.class, accion -> ((AccionDesconocida) accion).datos());

    private AccionDeAvisoJson() {
    }

    public static List<AccionDeAviso> leerTodas(List<Object> acciones) {
        return acciones.stream().map(AccionDeAvisoJson::leer).toList();
    }

    public static List<Object> escribirTodas(List<AccionDeAviso> acciones) {
        return acciones.stream().<Object>map(AccionDeAvisoJson::escribir).toList();
    }

    public static AccionDeAviso leer(Object json) {
        Map<String, Object> datos = comoMapa(json);
        String tipo = String.valueOf(datos.get("tipo"));
        return LECTORES.getOrDefault(tipo, desconocida -> new AccionDesconocida(tipo, datos)).apply(datos);
    }

    public static Map<String, Object> escribir(AccionDeAviso accion) {
        return ESCRITORES.get(accion.getClass()).apply(accion);
    }

    private static AccionDeAviso leerAbrir(Map<String, Object> datos) {
        return new AccionAbrirRegistro(texto(datos), proyectoId(datos), bloqueId(datos), registroId(datos));
    }

    private static AccionDeAviso leerRegistrar(Map<String, Object> datos) {
        return new AccionRegistrar(texto(datos), proyectoId(datos), bloqueId(datos));
    }

    private static AccionDeAviso leerVerZona(Map<String, Object> datos) {
        return new AccionVerZona(texto(datos), (String) datos.get("producto"));
    }

    private static AccionDeAviso leerEditar(Map<String, Object> datos) {
        if (registroId(datos) == null) {
            return invalida(datos, SIN_REGISTRO);
        }
        if (valores(datos).isEmpty()) {
            return invalida(datos, SIN_VALORES);
        }
        return new AccionEditarRegistro(
                texto(datos), proyectoId(datos), bloqueId(datos), registroId(datos), valores(datos));
    }

    private static AccionDeAviso leerCrear(Map<String, Object> datos) {
        if (bloqueId(datos) == null) {
            return invalida(datos, SIN_TABLA);
        }
        if (valores(datos).isEmpty()) {
            return invalida(datos, SIN_VALORES);
        }
        return new AccionCrearRegistro(texto(datos), proyectoId(datos), bloqueId(datos), valores(datos));
    }

    private static AccionDeAviso invalida(Map<String, Object> datos, String motivo) {
        return new AccionInvalida(String.valueOf(datos.get("tipo")), motivo, datos);
    }

    private static Map<String, Object> escribirAbrir(AccionAbrirRegistro accion) {
        Map<String, Object> json = nueva("ABRIR", accion.texto(), accion.proyectoId(), accion.bloqueId());
        poner(json, "registroId", accion.registroId());
        return json;
    }

    private static Map<String, Object> escribirRegistrar(AccionRegistrar accion) {
        return nueva("REGISTRAR", accion.texto(), accion.proyectoId(), accion.bloqueId());
    }

    private static Map<String, Object> escribirVerZona(AccionVerZona accion) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("tipo", "VER_ZONA");
        json.put("texto", accion.texto());
        json.put("producto", accion.producto());
        return json;
    }

    private static Map<String, Object> escribirEditar(AccionEditarRegistro accion) {
        Map<String, Object> json = nueva("EDITAR_REGISTRO", accion.texto(), accion.proyectoId(), accion.bloqueId());
        poner(json, "registroId", accion.registroId());
        json.put("valores", accion.valores());
        return json;
    }

    private static Map<String, Object> escribirCrear(AccionCrearRegistro accion) {
        Map<String, Object> json = nueva("CREAR_REGISTRO", accion.texto(), accion.proyectoId(), accion.bloqueId());
        json.put("valores", accion.valores());
        return json;
    }

    private static Map<String, Object> nueva(String tipo, String texto, ProyectoId proyectoId, BloqueId bloqueId) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("tipo", tipo);
        json.put("texto", texto);
        poner(json, "proyectoId", proyectoId);
        poner(json, "bloqueId", bloqueId);
        return json;
    }

    private static void poner(Map<String, Object> json, String clave, Object id) {
        Optional.ofNullable(id).ifPresent(valor -> json.put(clave, valor.toString()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comoMapa(Object json) {
        return json instanceof Map<?, ?> mapa ? (Map<String, Object>) mapa : Map.of();
    }

    private static String texto(Map<String, Object> datos) {
        return datos.get("texto") instanceof String texto ? texto : null;
    }

    private static ProyectoId proyectoId(Map<String, Object> datos) {
        return identificador(datos, "proyectoId", ProyectoId::de);
    }

    private static BloqueId bloqueId(Map<String, Object> datos) {
        return identificador(datos, "bloqueId", BloqueId::de);
    }

    private static RegistroId registroId(Map<String, Object> datos) {
        return identificador(datos, "registroId", RegistroId::de);
    }

    private static <T> T identificador(Map<String, Object> datos, String clave, Function<String, T> fabrica) {
        try {
            return fabrica.apply(String.valueOf(datos.get(clave)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<String, Object> valores(Map<String, Object> datos) {
        Map<String, Object> valores = new LinkedHashMap<>();
        if (datos.get("valores") instanceof Map<?, ?> mapa) {
            mapa.forEach((clave, valor) -> valores.put(String.valueOf(clave), valor));
        }
        return valores;
    }
}
