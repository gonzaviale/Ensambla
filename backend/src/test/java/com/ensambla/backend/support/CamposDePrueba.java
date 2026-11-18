package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoFormula;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.util.List;

/** Campos de dominio listos para los tests; cada uno nace con un id propio. */
public final class CamposDePrueba {

    private static final List<String> OPCIONES_POR_DEFECTO = List.of("Minorista", "Mayorista");

    private CamposDePrueba() {
    }

    public static Campo unCampoDeTipo(TipoCampo tipo) {
        return Campo.builder().id(CampoId.nuevo()).tipo(tipo).opciones(OPCIONES_POR_DEFECTO).build();
    }

    public static Campo unCampoNumerico(String nombre) {
        return unCampoNombrado(TipoCampo.NUMERO, nombre);
    }

    public static Campo unCampoDeMoneda(String nombre) {
        return unCampoNombrado(TipoCampo.MONEDA, nombre);
    }

    public static Campo unCampoDeTexto(String nombre) {
        return unCampoNombrado(TipoCampo.TEXTO, nombre);
    }

    public static Campo unCampoDeOpcion(String nombre, List<String> opciones) {
        return Campo.builder().id(CampoId.nuevo()).tipo(TipoCampo.OPCION).nombre(nombre).opciones(opciones).build();
    }

    public static Campo unaConexion(String nombre, BloqueId tablaRelacionada) {
        return Campo.builder()
                .id(CampoId.nuevo())
                .tipo(TipoCampo.RELACION)
                .nombre(nombre)
                .bloqueRelacionadoId(tablaRelacionada)
                .build();
    }

    public static CampoFormula unaFormulaQueUsa(Campo campo, String nombre) {
        return unaFormula(nombre, referenciaA(campo) + " * 2");
    }

    public static CampoFormula unaFormulaQueNoUsaCampos(String nombre) {
        return unaFormula(nombre, "2 * 2");
    }

    public static CampoFormula unaFormula(String nombre, String expresion) {
        return (CampoFormula) Campo.builder()
                .id(CampoId.nuevo())
                .tipo(TipoCampo.FORMULA)
                .nombre(nombre)
                .formula(expresion)
                .build();
    }

    public static CampoFormula unaFormulaQueSeCitaASiMisma(String nombre) {
        CampoId id = CampoId.nuevo();
        return (CampoFormula) Campo.builder()
                .id(id)
                .tipo(TipoCampo.FORMULA)
                .nombre(nombre)
                .formula("{" + id + "} * 2")
                .build();
    }

    public static String referenciaA(Campo campo) {
        return "{" + campo.getId() + "}";
    }

    private static Campo unCampoNombrado(TipoCampo tipo, String nombre) {
        return Campo.builder().id(CampoId.nuevo()).tipo(tipo).nombre(nombre).build();
    }
}
