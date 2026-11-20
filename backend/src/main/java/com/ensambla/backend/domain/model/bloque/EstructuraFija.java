package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.seccion.SeccionId;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lo mínimo que todo negocio necesita para que Caja, Objetivos, Avisos y Mi negocio funcionen: una sección de cada
 * tipo obligatorio y, en cada sección, las columnas fijas de su tipo. Completa lo que falta sin tocar los datos que ya
 * hay.
 */
public final class EstructuraFija {

    private static final String SUFIJO_DE_LA_ANTERIOR = "_anterior";
    private static final String NOMBRE_DE_LA_ANTERIOR = " (anterior)";

    private EstructuraFija() {
    }

    public static boolean estaCompleta(Sistema sistema) {
        List<Bloque> bloques = sistema.proyecto().getBloques();
        return tiposObligatoriosQueFaltanEn(bloques).isEmpty()
                && bloques.stream().allMatch(bloque -> tieneTodasSusColumnas(bloque, bloques));
    }

    public static Sistema completar(Sistema sistema, OffsetDateTime ahora) {
        Proyecto proyecto = sistema.proyecto();
        List<Bloque> bloques = new ArrayList<>(proyecto.getBloques());
        List<Bloque> nuevos = crearLasSeccionesQueFaltan(sistema, bloques, ahora);
        bloques.addAll(nuevos);
        bloques.forEach(bloque -> completarLasColumnasDe(bloque, bloques, ahora));
        proyecto.setBloques(bloques);
        return new Sistema(proyecto, sistema.secciones(), registrosConLosDe(nuevos, sistema));
    }

    /** Si la columna es una de las fijas de su tipo de sección: no se puede quitar ni cambiar de tipo de dato. */
    public static boolean esColumnaFija(Bloque bloque, Campo campo) {
        return bloque.getTipo().columnasFijas().stream().anyMatch(columna -> columna.esLaDe(campo));
    }

    /** Si la sección se puede quitar: no lo es la última de un tipo obligatorio. */
    public static boolean sePuedeQuitar(Bloque bloque, List<Bloque> bloques) {
        return !bloque.getTipo().esObligatorioEnElNegocio() || bloques.stream()
                .anyMatch(otro -> !otro.getId().equals(bloque.getId()) && otro.getTipo() == bloque.getTipo());
    }

    private static List<TipoBloque> tiposObligatoriosQueFaltanEn(List<Bloque> bloques) {
        return TipoBloque.obligatorios().stream()
                .filter(tipo -> bloques.stream().noneMatch(bloque -> bloque.getTipo() == tipo))
                .toList();
    }

    private static boolean tieneTodasSusColumnas(Bloque bloque, List<Bloque> bloques) {
        return bloque.getTipo().columnasFijas().stream().allMatch(columna -> bloque.getCampos().stream()
                .anyMatch(campo -> columna.esLaDe(campo) && columna.laCumple(campo, bloques)));
    }

    private static List<Bloque> crearLasSeccionesQueFaltan(Sistema sistema, List<Bloque> bloques,
                                                           OffsetDateTime ahora) {
        List<Bloque> nuevos = new ArrayList<>();
        for (TipoBloque tipo : tiposObligatoriosQueFaltanEn(bloques)) {
            nuevos.add(bloqueNuevo(tipo, sistema, (short) (bloques.size() + nuevos.size()), ahora));
        }
        return nuevos;
    }

    private static Bloque bloqueNuevo(TipoBloque tipo, Sistema sistema, short posicion, OffsetDateTime ahora) {
        Proyecto proyecto = sistema.proyecto();
        return Bloque.builder()
                .id(BloqueId.nuevo())
                .negocioId(proyecto.getNegocioId())
                .proyectoId(proyecto.getId())
                .seccionId(primeraSeccionDe(sistema))
                .nombre(tipo.nombreInicial())
                .posicion(posicion)
                .tipo(tipo)
                .color(tipo.colorInicial())
                .direccionStock(tipo.direccionDeStock())
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    private static SeccionId primeraSeccionDe(Sistema sistema) {
        return sistema.secciones().stream()
                .min(Comparator.comparing(Seccion::getPosicion))
                .map(Seccion::getId)
                .orElse(null);
    }

    private static Map<BloqueId, List<Registro>> registrosConLosDe(List<Bloque> nuevos, Sistema sistema) {
        Map<BloqueId, List<Registro>> registros = new LinkedHashMap<>(sistema.registrosPorBloque());
        nuevos.forEach(bloque -> registros.put(bloque.getId(), List.of()));
        return registros;
    }

    private static void completarLasColumnasDe(Bloque bloque, List<Bloque> bloques, OffsetDateTime ahora) {
        List<Campo> campos = new ArrayList<>(bloque.getCampos());
        bloque.getTipo().columnasFijas()
                .forEach(columna -> completarLaColumna(columna, campos, bloque, bloques, ahora));
        bloque.setCampos(campos);
    }

    private static void completarLaColumna(ColumnaFija columna, List<Campo> campos, Bloque bloque,
                                           List<Bloque> bloques, OffsetDateTime ahora) {
        Optional<Campo> existente = campos.stream().filter(columna::esLaDe).findFirst();
        existente.ifPresentOrElse(
                propia -> repararLaColumna(columna, propia, campos, bloque, bloques, ahora),
                () -> agregarLaColumna(columna, campos, bloque, bloques, ahora));
    }

    private static void repararLaColumna(ColumnaFija columna, Campo propia, List<Campo> campos, Bloque bloque,
                                         List<Bloque> bloques, OffsetDateTime ahora) {
        if (!columna.esDelTipoCorrecto(propia, bloques)) {
            apartarYReponer(columna, propia, campos, bloque, bloques, ahora);
            return;
        }
        if (!columna.cumpleLoDemas(propia)) {
            campos.set(campos.indexOf(propia), columna.ajustada(propia, ahora));
        }
    }

    private static void apartarYReponer(ColumnaFija columna, Campo propia, List<Campo> campos, Bloque bloque,
                                        List<Bloque> bloques, OffsetDateTime ahora) {
        apartar(propia, ahora);
        agregarLaColumna(columna, campos, bloque, bloques, ahora);
    }

    /** La columna de la persona con una clave reservada y otro tipo se conserva, con sus datos, con otro nombre. */
    private static void apartar(Campo propia, OffsetDateTime ahora) {
        propia.setClave(propia.getClave() + SUFIJO_DE_LA_ANTERIOR);
        propia.setNombre(propia.getNombre() + NOMBRE_DE_LA_ANTERIOR);
        propia.setActualizadoEn(ahora);
    }

    private static void agregarLaColumna(ColumnaFija columna, List<Campo> campos, Bloque bloque,
                                         List<Bloque> bloques, OffsetDateTime ahora) {
        CampoId id = idLibreParaLa(columna, campos, bloque);
        campos.add(columna.nueva(bloque, bloques, id, siguientePosicionEn(campos), ahora));
    }

    /** El id que la pantalla le va a calcular a la clave; si ya lo usa otra columna del bloque, uno nuevo. */
    private static CampoId idLibreParaLa(ColumnaFija columna, List<Campo> campos, Bloque bloque) {
        CampoId derivado = IdSistema.aId(bloque.getId(), columna.clave(), CampoId::de);
        boolean ocupado = campos.stream().anyMatch(campo -> derivado.equals(campo.getId()));
        return ocupado ? CampoId.nuevo() : derivado;
    }

    private static short siguientePosicionEn(List<Campo> campos) {
        return (short) campos.stream().mapToInt(campo -> campo.getPosicion() + 1).max().orElse(0);
    }
}
