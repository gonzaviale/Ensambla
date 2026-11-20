package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;

import java.util.List;

public interface ProgresoObjetivoRepository {

    /** Guarda la foto del día; si ya había una del mismo sujeto y día, la pisa. */
    void registrar(ProgresoObjetivo progreso);

    /**
     * Las fotos del objetivo y de sus mini-objetivos, de la más vieja a la más nueva. No filtra por negocio: quien
     * llama ya comprobó que el objetivo es del negocio de la sesión.
     */
    List<ProgresoObjetivo> findAllByObjetivoId(ObjetivoId objetivoId);
}
