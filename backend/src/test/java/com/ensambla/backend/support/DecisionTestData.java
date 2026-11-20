package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;

public final class DecisionTestData {

    public static final DecisionObjetivoId DECISION_ID = DecisionObjetivoId.de("00000000-0000-4000-8000-0000000000d1");
    public static final DecisionObjetivoId OTRA_DECISION_ID =
            DecisionObjetivoId.de("00000000-0000-4000-8000-0000000000d2");
    public static final AvisoId AVISO_ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");
    public static final String TITULO_AVISO = "Bajá el precio de la Yerba";
    /** El lunes de la semana de {@code AHORA} (martes 10/11/2026). */
    public static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    private DecisionTestData() {
    }

    /** Un aviso del producto de los mini-objetivos de prueba, ya aplicado. */
    public static Aviso aviso(TipoAviso tipo, EstadoAviso estado) {
        return Aviso.builder()
                .id(AVISO_ID)
                .negocioId(NEGOCIO_ID)
                .tipo(tipo)
                .entidad("registro:" + PRODUCTO_ID)
                .titulo(TITULO_AVISO)
                .estado(estado)
                .resueltoEn(AHORA)
                .build();
    }

    /** Lo que se vendía antes y después: sube el ingreso por día de 100 a 150 (+$350 por semana). */
    public static ImpactoAviso impacto() {
        return new ImpactoAviso(14, new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("100.00"),
                new BigDecimal("150.00"));
    }

    /** Un aviso aplicado de un producto, anotado en el objetivo de prueba y vinculado a su mini-objetivo. */
    public static DecisionObjetivo decision(TipoDecision tipo, AvisoId avisoId) {
        return new DecisionObjetivo(DECISION_ID, NEGOCIO_ID, OBJETIVO_ID, MINI_ID, tipo, avisoId, PRODUCTO_ID,
                TITULO_AVISO, AHORA, SEMANA);
    }
}
