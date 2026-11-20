package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Una columna que Ensambla necesita en toda sección de cierto tipo: la leen Caja, Objetivos, Avisos o Mi negocio. La
 * persona puede cambiarle el nombre, pero no quitarla ni cambiarle el tipo de dato.
 *
 * @param tipos los tipos de dato que la columna admite; el primero es el que lleva al crearla
 * @param opcionesIniciales las opciones con las que nace una columna de lista
 * @param opcionesRequeridas las opciones de una columna de lista que no se pueden quitar
 * @param destino el tipo de sección al que apunta si es una conexión; {@code null} si no lo es
 * @param paraQue lo que Ensambla hace con la columna, para explicarlo cuando se intenta quitarla
 */
public record ColumnaFija(
        String clave,
        String nombre,
        List<TipoCampo> tipos,
        boolean obligatoria,
        List<String> opcionesIniciales,
        List<String> opcionesRequeridas,
        TipoBloque destino,
        String paraQue) {

    public static ColumnaFija texto(String clave, String nombre, String paraQue) {
        return de(clave, nombre, List.of(TipoCampo.TEXTO), paraQue);
    }

    public static ColumnaFija numero(String clave, String nombre, String paraQue) {
        return de(clave, nombre, List.of(TipoCampo.NUMERO), paraQue);
    }

    public static ColumnaFija moneda(String clave, String nombre, String paraQue) {
        return de(clave, nombre, List.of(TipoCampo.MONEDA), paraQue);
    }

    public static ColumnaFija fecha(String clave, String nombre, String paraQue) {
        return de(clave, nombre, List.of(TipoCampo.FECHA), paraQue);
    }

    public static ColumnaFija siNo(String clave, String nombre, String paraQue) {
        return de(clave, nombre, List.of(TipoCampo.SI_NO), paraQue);
    }

    public static ColumnaFija lista(String clave, String nombre, String paraQue, List<String> iniciales,
                                    List<String> requeridas) {
        return new ColumnaFija(clave, nombre, List.of(TipoCampo.OPCION), false, iniciales, requeridas, null, paraQue);
    }

    public static ColumnaFija vinculo(String clave, String nombre, TipoBloque destino, String paraQue) {
        return new ColumnaFija(clave, nombre, List.of(TipoCampo.RELACION), false, List.of(), List.of(), destino,
                paraQue);
    }

    /** La columna es un texto con el nombre, o una conexión a una fila del tipo de sección {@code destino}. */
    public static ColumnaFija textoOVinculo(String clave, String nombre, TipoBloque destino, String paraQue) {
        return new ColumnaFija(clave, nombre, List.of(TipoCampo.TEXTO, TipoCampo.RELACION), false, List.of(),
                List.of(), destino, paraQue);
    }

    private static ColumnaFija de(String clave, String nombre, List<TipoCampo> tipos, String paraQue) {
        return new ColumnaFija(clave, nombre, tipos, false, List.of(), List.of(), null, paraQue);
    }

    public ColumnaFija siempreObligatoria() {
        return new ColumnaFija(clave, nombre, tipos, true, opcionesIniciales, opcionesRequeridas, destino, paraQue);
    }

    public boolean esLaDe(Campo campo) {
        return clave.equals(campo.getClave());
    }

    public boolean laCumple(Campo campo, List<Bloque> bloques) {
        return esDelTipoCorrecto(campo, bloques) && cumpleLoDemas(campo);
    }

    /** El tipo de dato y el destino de la conexión: lo que no se puede arreglar sin perder los datos de la columna. */
    public boolean esDelTipoCorrecto(Campo campo, List<Bloque> bloques) {
        return tipos.contains(campo.getTipo()) && apuntaAlDestinoCorrecto(campo, bloques);
    }

    /** Que sea obligatoria y conserve sus opciones: lo que sí se puede arreglar sin tocar los datos. */
    public boolean cumpleLoDemas(Campo campo) {
        return cumpleLaObligatoriedadDe(campo) && tieneLasOpcionesRequeridas(campo);
    }

    public Campo nueva(Bloque bloque, List<Bloque> bloques, CampoId id, short posicion, OffsetDateTime ahora) {
        return Campo.builder()
                .id(id)
                .negocioId(bloque.getNegocioId())
                .bloqueId(bloque.getId())
                .nombre(nombre)
                .tipo(tipos.get(0))
                .obligatorio(obligatoria)
                .opciones(opcionesIniciales)
                .bloqueRelacionadoId(destinoInicialEn(bloques))
                .clave(clave)
                .posicion(posicion)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    /** La misma columna de la persona, con lo que le faltaba para cumplir (obligatoria, opciones); no pierde datos. */
    public Campo ajustada(Campo campo, OffsetDateTime ahora) {
        return Campo.builder()
                .id(campo.getId())
                .negocioId(campo.getNegocioId())
                .bloqueId(campo.getBloqueId())
                .nombre(campo.getNombre())
                .tipo(campo.getTipo())
                .obligatorio(obligatoria || campo.esObligatorio())
                .valorInicial(campo.getValorInicial())
                .opciones(opcionesCompletasDe(campo))
                .bloqueRelacionadoId(campo.getBloqueRelacionadoId())
                .clave(campo.getClave())
                .minimo(campo.getMinimo())
                .posicion(campo.getPosicion())
                .creadoEn(campo.getCreadoEn())
                .actualizadoEn(ahora)
                .build();
    }

    public String mensajeAlQuitar(String nombreActual) {
        return "No se puede quitar «" + nombreActual + "»: Ensambla la necesita para " + paraQue + ".";
    }

    public String mensajeAlCambiar(String nombreActual) {
        return "No se puede cambiar «" + nombreActual + "»: Ensambla la necesita para " + paraQue
                + ". Podés cambiarle el nombre.";
    }

    private boolean apuntaAlDestinoCorrecto(Campo campo, List<Bloque> bloques) {
        return destino == null || !campo.esRelacion() || bloques.stream()
                .anyMatch(bloque -> bloque.getId().equals(campo.getBloqueRelacionadoId())
                        && bloque.getTipo() == destino);
    }

    private boolean cumpleLaObligatoriedadDe(Campo campo) {
        return !obligatoria || campo.esObligatorio();
    }

    private boolean tieneLasOpcionesRequeridas(Campo campo) {
        return campo.getOpciones().containsAll(opcionesRequeridas);
    }

    private List<String> opcionesCompletasDe(Campo campo) {
        List<String> opciones = new ArrayList<>(campo.getOpciones());
        opcionesRequeridas.stream().filter(opcion -> !opciones.contains(opcion)).forEach(opciones::add);
        return opciones;
    }

    private BloqueId destinoInicialEn(List<Bloque> bloques) {
        if (destino == null || tipos.get(0) != TipoCampo.RELACION) {
            return null;
        }
        return bloques.stream().filter(bloque -> bloque.getTipo() == destino).map(Bloque::getId).findFirst()
                .orElse(null);
    }
}
