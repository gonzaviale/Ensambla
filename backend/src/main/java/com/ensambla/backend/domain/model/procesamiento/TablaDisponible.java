package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.util.Optional;
import java.util.Set;

/**
 * Una tabla del negocio donde se pueden cargar registros, con el nombre del proyecto al que pertenece y su tipo.
 *
 * @param tipo cómo se comporta la tabla: las de cosas (productos, clientes) se actualizan, las de movimientos
 *             (ventas, gastos) suman una fila por mensaje
 */
public record TablaDisponible(BloqueLectura bloque, String proyecto, TipoBloque tipo) {

    /** Los tipos cuyas filas son «cosas» que se nombran y se vuelven a mencionar: no un movimiento más. */
    private static final Set<TipoBloque> DE_COSAS = Set.of(
            TipoBloque.INVENTARIO, TipoBloque.CLIENTES, TipoBloque.FORMULARIO);

    /** Una tabla personalizada (formulario): se la trata como una tabla de cosas. */
    public TablaDisponible(BloqueLectura bloque, String proyecto) {
        this(bloque, proyecto, TipoBloque.FORMULARIO);
    }

    /** Cómo se la nombra en el chat: proyecto, una flecha y la tabla. */
    public String etiqueta() {
        return proyecto + " › " + bloque.nombre();
    }

    /** Si un mensaje que nombra una fila que ya existe la actualiza, en vez de sumar otra. */
    public boolean actualizaFilasExistentes() {
        return DE_COSAS.contains(tipo);
    }

    /** La columna que nombra a cada fila: la primera de texto. */
    public Optional<CampoConocido> campoIdentificador() {
        return bloque.campos().stream().filter(campo -> campo.tipo() == TipoCampo.TEXTO).findFirst();
    }
}
