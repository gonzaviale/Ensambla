package com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.FaltanDatosMiniObjetivos;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Los mini-objetivos de un objetivo.
 *
 * @param minis       los que ocupan un lugar, por posición: los en curso y los logrados que esperan el cierre del
 *                    domingo (se ven como «¡Lo lograste!» hasta que Sami les sube la meta o los cambia)
 * @param historial   los ya reajustados y los quitados, del más nuevo al más viejo
 * @param faltanDatos por qué no hay 3 lugares ocupados; vacío si los hay
 */
public record ListarMiniObjetivosResponse(
        List<MiniObjetivoItem> minis,
        List<MiniObjetivoItem> historial,
        FaltanDatosMiniObjetivos faltanDatos) {

    public static ListarMiniObjetivosResponse de(Objetivo objetivo, List<MiniObjetivo> miniObjetivos) {
        Set<Integer> enCurso = miniObjetivos.stream()
                .filter(MiniObjetivo::estaEnCurso)
                .map(MiniObjetivo::getPosicion)
                .collect(Collectors.toSet());
        List<MiniObjetivo> ocupan = miniObjetivos.stream()
                .filter(mini -> mini.estaEnCurso() || esperaElCierre(mini, enCurso))
                .toList();
        List<MiniObjetivoItem> minis = ocupan.stream()
                .sorted(Comparator.comparingInt(MiniObjetivo::getPosicion))
                .map(MiniObjetivoItem::de)
                .toList();
        List<MiniObjetivoItem> historial = miniObjetivos.stream()
                .filter(mini -> !ocupan.contains(mini))
                .sorted(Comparator.comparing(MiniObjetivo::getCreadoEn).reversed())
                .map(MiniObjetivoItem::de)
                .toList();
        return new ListarMiniObjetivosResponse(minis, historial, faltanDatos(objetivo, minis.size()));
    }

    /** Un logrado sigue en su lugar hasta que el domingo Sami lo reajusta, salvo que ya haya otro en curso ahí. */
    private static boolean esperaElCierre(MiniObjetivo mini, Set<Integer> posicionesEnCurso) {
        return mini.getEstado() == EstadoMiniObjetivo.LOGRADO && mini.esperaReajuste()
                && !posicionesEnCurso.contains(mini.getPosicion());
    }

    private static FaltanDatosMiniObjetivos faltanDatos(Objetivo objetivo, int enCurso) {
        if (objetivo.estaAlcanzado()) {
            return FaltanDatosMiniObjetivos.OBJETIVO_ALCANZADO;
        }
        return enCurso < MiniObjetivo.MAXIMO_EN_CURSO ? FaltanDatosMiniObjetivos.POCOS_DATOS : null;
    }
}
