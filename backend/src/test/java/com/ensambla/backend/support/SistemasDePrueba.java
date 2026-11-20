package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.EstructuraFija;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.domain.model.sistema.Sistema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;

/** Sistemas armados en memoria para probar la estructura fija del negocio. */
public final class SistemasDePrueba {

    private static final SeccionId SECCION_ID = SeccionId.de(UUID.fromString("00000000-0000-4000-8000-0000000000c1"));

    private SistemasDePrueba() {
    }

    public static Sistema unSistemaVacio() {
        return unSistemaConPestanas(List.of(unaPestana()));
    }

    public static Sistema unSistemaSinPestanas() {
        return unSistemaConPestanas(List.of());
    }

    public static Sistema unSistemaCompleto() {
        return EstructuraFija.completar(unSistemaVacio(), AHORA);
    }

    public static Sistema unSistemaCon(Bloque... bloques) {
        Sistema vacio = unSistemaVacio();
        vacio.proyecto().setBloques(new ArrayList<>(List.of(bloques)));
        return vacio;
    }

    public static Sistema unaCopiaDe(Sistema sistema) {
        Proyecto original = sistema.proyecto();
        Proyecto copia = Proyecto.builder().id(original.getId()).negocioId(original.getNegocioId())
                .nombre(original.getNombre()).creadoEn(AHORA).actualizadoEn(AHORA)
                .bloques(sistema.proyecto().getBloques().stream().map(SistemasDePrueba::unaCopiaDe).toList()).build();
        return new Sistema(copia, sistema.secciones(), new LinkedHashMap<>(sistema.registrosPorBloque()));
    }

    public static Sistema unSistemaConOtroId(Sistema sistema) {
        Proyecto original = sistema.proyecto();
        Proyecto otro = Proyecto.builder().id(ProyectoId.nuevo()).negocioId(original.getNegocioId())
                .nombre("Otro negocio").creadoEn(AHORA).actualizadoEn(AHORA).bloques(original.getBloques()).build();
        return new Sistema(otro, sistema.secciones(), new LinkedHashMap<>(sistema.registrosPorBloque()));
    }

    public static void renombrarLaColumna(Bloque bloque, String clave, String nombre) {
        Campo anterior = laColumna(bloque, clave);
        Campo renombrado = Campo.builder().id(anterior.getId()).bloqueId(bloque.getId()).nombre(nombre)
                .tipo(anterior.getTipo()).obligatorio(anterior.esObligatorio()).clave(clave)
                .posicion(anterior.getPosicion()).opciones(anterior.getOpciones())
                .bloqueRelacionadoId(anterior.getBloqueRelacionadoId()).build();
        reemplazar(bloque, anterior, renombrado);
    }

    public static void quitarLaSeccion(Sistema sistema, Bloque bloque) {
        List<Bloque> bloques = new ArrayList<>(sistema.proyecto().getBloques());
        bloques.removeIf(candidato -> candidato.getId().equals(bloque.getId()));
        sistema.proyecto().setBloques(bloques);
    }

    public static void cambiarElTipoDeLaSeccion(Bloque bloque, TipoBloque tipo) {
        bloque.setTipo(tipo);
    }

    public static Bloque unaSeccionVaciaDe(TipoBloque tipo) {
        return Bloque.builder().id(BloqueId.nuevo()).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID)
                .nombre(tipo.nombreInicial()).posicion((short) 0).tipo(tipo).campos(new ArrayList<>()).build();
    }

    public static Bloque laSeccionDe(Sistema sistema, TipoBloque tipo) {
        return sistema.proyecto().getBloques().stream().filter(bloque -> bloque.getTipo() == tipo).findFirst()
                .orElseThrow();
    }

    public static Campo laColumna(Bloque bloque, String clave) {
        return bloque.getCampos().stream().filter(campo -> clave.equals(campo.getClave())).findFirst().orElseThrow();
    }

    public static boolean tieneLaColumna(Bloque bloque, String clave) {
        return bloque.getCampos().stream().anyMatch(campo -> clave.equals(campo.getClave()));
    }

    public static void quitarLaColumna(Bloque bloque, String clave) {
        List<Campo> campos = new ArrayList<>(bloque.getCampos());
        campos.removeIf(campo -> clave.equals(campo.getClave()));
        bloque.setCampos(campos);
    }

    /** Reemplaza la columna de esa clave por otra con el mismo id y otro tipo de dato. */
    public static void cambiarElTipoDeLaColumna(Bloque bloque, String clave, TipoCampo tipo) {
        Campo anterior = laColumna(bloque, clave);
        reemplazar(bloque, anterior, unaColumna(bloque, clave, anterior.getNombre(), tipo, anterior.getId()));
    }

    public static void dejarDeExigirLaColumna(Bloque bloque, String clave) {
        Campo anterior = laColumna(bloque, clave);
        Campo opcional = Campo.builder().id(anterior.getId()).bloqueId(bloque.getId()).nombre(anterior.getNombre())
                .tipo(anterior.getTipo()).obligatorio(false).clave(clave).posicion(anterior.getPosicion())
                .opciones(anterior.getOpciones()).bloqueRelacionadoId(anterior.getBloqueRelacionadoId()).build();
        reemplazar(bloque, anterior, opcional);
    }

    public static void conLasOpciones(Bloque bloque, String clave, List<String> opciones) {
        Campo anterior = laColumna(bloque, clave);
        Campo conOpciones = Campo.builder().id(anterior.getId()).bloqueId(bloque.getId()).nombre(anterior.getNombre())
                .tipo(TipoCampo.OPCION).obligatorio(anterior.esObligatorio()).clave(clave)
                .posicion(anterior.getPosicion()).opciones(opciones).build();
        reemplazar(bloque, anterior, conOpciones);
    }

    public static void conLaConexionA(Bloque bloque, String clave, BloqueId destino) {
        Campo anterior = laColumna(bloque, clave);
        Campo conexion = Campo.builder().id(anterior.getId()).bloqueId(bloque.getId()).nombre(anterior.getNombre())
                .tipo(TipoCampo.RELACION).obligatorio(anterior.esObligatorio()).clave(clave)
                .posicion(anterior.getPosicion()).bloqueRelacionadoId(destino).build();
        reemplazar(bloque, anterior, conexion);
    }

    public static Campo unaColumnaPropia(Bloque bloque, String clave, String nombre, TipoCampo tipo) {
        return unaColumna(bloque, clave, nombre, tipo, CampoId.nuevo());
    }

    public static void agregarLaColumna(Bloque bloque, Campo campo) {
        List<Campo> campos = new ArrayList<>(bloque.getCampos());
        campos.add(campo);
        bloque.setCampos(campos);
    }

    private static Campo unaColumna(Bloque bloque, String clave, String nombre, TipoCampo tipo, CampoId id) {
        return Campo.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(bloque.getId()).nombre(nombre).tipo(tipo)
                .obligatorio(true).clave(clave).posicion((short) bloque.getCampos().size())
                .opciones(List.of("unidad", "peso", "Mercadería")).build();
    }

    private static void reemplazar(Bloque bloque, Campo anterior, Campo nuevo) {
        List<Campo> campos = new ArrayList<>(bloque.getCampos());
        campos.set(campos.indexOf(anterior), nuevo);
        bloque.setCampos(campos);
    }

    private static Sistema unSistemaConPestanas(List<Seccion> pestanas) {
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).nombre("Mi negocio")
                .creadoEn(AHORA).actualizadoEn(AHORA).bloques(new ArrayList<>()).build();
        return new Sistema(proyecto, pestanas, new LinkedHashMap<>());
    }

    private static Seccion unaPestana() {
        return Seccion.builder().id(SECCION_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).nombre("Vista general")
                .posicion((short) 0).creadoEn(AHORA).actualizadoEn(AHORA).build();
    }

    private static Bloque unaCopiaDe(Bloque bloque) {
        return Bloque.builder().id(bloque.getId()).negocioId(bloque.getNegocioId()).proyectoId(bloque.getProyectoId())
                .nombre(bloque.getNombre()).posicion(bloque.getPosicion()).seccionId(bloque.getSeccionId())
                .tipo(bloque.getTipo()).color(bloque.getColor()).ancho(bloque.getAncho())
                .campos(new ArrayList<>(bloque.getCampos())).build();
    }
}
