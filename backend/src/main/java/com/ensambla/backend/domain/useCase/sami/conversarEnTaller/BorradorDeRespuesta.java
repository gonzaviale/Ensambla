package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.HerramientaTaller;
import com.ensambla.backend.domain.model.sami.taller.OpcionesTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;

import java.util.ArrayList;
import java.util.List;

/** La respuesta de Sami mientras se arma: se le va sumando lo que piden las consultas extra del turno. */
final class BorradorDeRespuesta {

    private String texto;
    private final List<AccionTaller> acciones;
    private List<OpcionesTaller> preguntas;
    private int renglonesSinCargar;

    BorradorDeRespuesta(String texto, List<AccionTaller> acciones, List<OpcionesTaller> preguntas,
                        int renglonesSinCargar) {
        this.texto = texto;
        this.acciones = new ArrayList<>(acciones);
        this.preguntas = preguntas;
        this.renglonesSinCargar = renglonesSinCargar;
    }

    boolean sinRegistros() {
        return acciones.stream().noneMatch(accion -> accion.es(HerramientaTaller.CARGAR_REGISTROS));
    }

    boolean borraRegistros() {
        return acciones.stream().anyMatch(accion -> accion.es(HerramientaTaller.BORRAR_REGISTROS));
    }

    boolean sinPreguntas() {
        return preguntas.isEmpty();
    }

    void sumar(List<AccionTaller> nuevas, String frase) {
        acciones.addAll(nuevas);
        texto = texto + " " + frase + ".";
    }

    void contarSinCargar(int renglones) {
        renglonesSinCargar += renglones;
    }

    void reemplazarPreguntas(List<OpcionesTaller> nuevas) {
        preguntas = nuevas;
    }

    /** Sami nunca afirma que cargó todo si parte de lo que leyó no se pudo cargar. */
    RespuestaTaller comoRespuesta() {
        return new RespuestaTaller(textoFinal(), List.copyOf(acciones), preguntas);
    }

    private String textoFinal() {
        return renglonesSinCargar > 0 ? texto + " " + avisoDeRenglonesSinCargar() : texto;
    }

    private String avisoDeRenglonesSinCargar() {
        String cuantos = renglonesSinCargar == 1 ? "1 renglón" : renglonesSinCargar + " renglones";
        return "No pude cargar " + cuantos + " de la foto: faltan en la tabla, revisalos.";
    }
}
