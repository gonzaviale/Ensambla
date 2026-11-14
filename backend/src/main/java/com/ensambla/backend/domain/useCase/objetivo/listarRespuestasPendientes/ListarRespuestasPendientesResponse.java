package com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Los mini-objetivos manuales que esperan la respuesta de la persona, del más viejo al más nuevo. */
public record ListarRespuestasPendientesResponse(List<Pendiente> pendientes) {

    public record Pendiente(ObjetivoId objetivoId, String objetivoTexto, MiniObjetivoItem miniObjetivo) {
    }

    /** {@code minis} son de objetivos del negocio, y {@code objetivos} son todos los del negocio. */
    public static ListarRespuestasPendientesResponse de(List<Objetivo> objetivos, List<MiniObjetivo> minis) {
        Map<ObjetivoId, Objetivo> porId = objetivos.stream()
                .collect(Collectors.toMap(Objetivo::getId, Function.identity()));
        return new ListarRespuestasPendientesResponse(minis.stream()
                .map(mini -> new Pendiente(
                        mini.getObjetivoId(), porId.get(mini.getObjetivoId()).getTexto(), MiniObjetivoItem.de(mini)))
                .toList());
    }
}
