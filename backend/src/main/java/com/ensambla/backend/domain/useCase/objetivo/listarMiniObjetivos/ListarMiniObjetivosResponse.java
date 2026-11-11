package com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos;

import com.ensambla.backend.domain.model.objetivo.FaltanDatosMiniObjetivos;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;

import java.util.Comparator;
import java.util.List;

/**
 * Los mini-objetivos de un objetivo.
 *
 * @param minis       los que están en curso, por posición
 * @param historial   los logrados, reemplazados y quitados, del más nuevo al más viejo
 * @param faltanDatos por qué no hay 3 en curso; vacío si los hay
 */
public record ListarMiniObjetivosResponse(
        List<MiniObjetivoItem> minis,
        List<MiniObjetivoItem> historial,
        FaltanDatosMiniObjetivos faltanDatos) {

    public static ListarMiniObjetivosResponse de(Objetivo objetivo, List<MiniObjetivo> miniObjetivos) {
        List<MiniObjetivoItem> minis = miniObjetivos.stream()
                .filter(MiniObjetivo::estaEnCurso)
                .sorted(Comparator.comparingInt(MiniObjetivo::getPosicion))
                .map(MiniObjetivoItem::de)
                .toList();
        List<MiniObjetivoItem> historial = miniObjetivos.stream()
                .filter(mini -> !mini.estaEnCurso())
                .sorted(Comparator.comparing(MiniObjetivo::getCreadoEn).reversed())
                .map(MiniObjetivoItem::de)
                .toList();
        return new ListarMiniObjetivosResponse(minis, historial, faltanDatos(objetivo, minis.size()));
    }

    private static FaltanDatosMiniObjetivos faltanDatos(Objetivo objetivo, int enCurso) {
        if (objetivo.estaAlcanzado()) {
            return FaltanDatosMiniObjetivos.OBJETIVO_ALCANZADO;
        }
        return enCurso < MiniObjetivo.MAXIMO_EN_CURSO ? FaltanDatosMiniObjetivos.POCOS_DATOS : null;
    }
}
