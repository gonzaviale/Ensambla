package com.ensambla.backend.domain.model.sami.taller;

import java.util.ArrayList;
import java.util.List;

/** Lo que se va acumulando mientras se validan las herramientas que pidió el modelo en un turno. */
public final class TurnoDelTaller {

    private final PiezasDelTurno piezas;
    private final List<AccionTaller> acciones = new ArrayList<>();
    private final List<OpcionesTaller> preguntas = new ArrayList<>();
    /** La pregunta del kit que se armó, por si el modelo no preguntó nada. */
    private OpcionesTaller preguntaDelKit;
    private int registrosDescartados;
    private int delModelo;

    public TurnoDelTaller(List<PiezaTaller> piezas) {
        this.piezas = new PiezasDelTurno(piezas);
    }

    public PiezasDelTurno piezas() {
        return piezas;
    }

    public boolean admiteOtraDelModelo() {
        return delModelo < CatalogoTaller.MAX_ACCIONES && acciones.size() < CatalogoTaller.MAX_ACCIONES_TOTALES;
    }

    public int lugar() {
        return CatalogoTaller.MAX_ACCIONES_TOTALES - acciones.size();
    }

    /** Las dudas válidas se juntan hasta el tope del turno; una repetida no se suma. */
    public void sumarPreguntas(List<OpcionesTaller> nuevas) {
        nuevas.stream().filter(nueva -> !yaPreguntada(nueva)).forEach(this::sumarSiHayLugar);
    }

    /** Una llamada del modelo cuenta una vez aunque se haya expandido en varias acciones. Las inválidas no cuentan. */
    public void sumarDelModelo(List<AccionTaller> hechas) {
        if (!hechas.isEmpty()) {
            delModelo++;
            acciones.addAll(hechas);
        }
    }

    public void guardarPreguntaDelKit(OpcionesTaller pregunta) {
        preguntaDelKit = pregunta;
    }

    public void sumarRegistrosDescartados(int cantidad) {
        registrosDescartados += cantidad;
    }

    /** Al armar con un kit Sami siempre propone cómo seguir: si el modelo no preguntó, va la pregunta del kit. */
    public ValidacionTaller validacion() {
        List<OpcionesTaller> dudas = preguntas.isEmpty() && preguntaDelKit != null
                ? List.of(preguntaDelKit)
                : List.copyOf(preguntas);
        return new ValidacionTaller(List.copyOf(acciones), dudas, registrosDescartados, piezas.comoLista());
    }

    private boolean yaPreguntada(OpcionesTaller nueva) {
        return preguntas.stream().anyMatch(pregunta -> pregunta.pregunta().equals(nueva.pregunta()));
    }

    private void sumarSiHayLugar(OpcionesTaller nueva) {
        if (preguntas.size() < CatalogoTaller.MAX_PREGUNTAS) {
            preguntas.add(nueva);
        }
    }
}
