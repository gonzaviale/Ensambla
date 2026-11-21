package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivoId;

import java.time.LocalDate;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;

public final class RespuestaMiniObjetivoTestData {

    public static final RespuestaMiniObjetivoId RESPUESTA_ID =
            RespuestaMiniObjetivoId.de("00000000-0000-4000-8000-000000000008");
    public static final String COMENTARIO = "Hablé con 1 sola vecina";
    /** El lunes de la semana de AHORA. */
    public static final LocalDate SEMANA_RESPUESTA = LocalDate.of(2026, 11, 9);

    private RespuestaMiniObjetivoTestData() {
    }

    /** La respuesta «lo hice en parte, no tuve tiempo» al mini-objetivo manual de prueba. */
    public static RespuestaMiniObjetivo respuesta() {
        return new RespuestaMiniObjetivo(
                RESPUESTA_ID,
                NEGOCIO_ID,
                OBJETIVO_ID,
                OTRO_MINI_ID,
                HizoManual.EN_PARTE,
                QuePaso.NO_TUVE_TIEMPO,
                COMENTARIO,
                SEMANA_RESPUESTA,
                AHORA);
    }
}
