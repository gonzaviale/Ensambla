package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;

public final class MiniObjetivoTestData {

    public static final MiniObjetivoId MINI_ID = MiniObjetivoId.de("00000000-0000-4000-8000-000000000006");
    public static final MiniObjetivoId OTRO_MINI_ID = MiniObjetivoId.de("00000000-0000-4000-8000-000000000007");
    public static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    public static final String PRODUCTO = "Yerba 1 kg";
    /** Un lunes: la semana en que se propone. */
    public static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);
    public static final BigDecimal VALOR_INICIAL = new BigDecimal("12600.00");
    public static final BigDecimal META_MINI = new BigDecimal("14183.00");
    public static final BigDecimal APORTE = new BigDecimal("6786.00");
    public static final String TITULO = "Subile $250 a Yerba 1 kg";
    public static final String JUSTIFICACION = "Te deja $900 por unidad y vendés cerca de 14 por semana.";

    private MiniObjetivoTestData() {
    }

    /** Un «subir precio» en curso en la posición 1, recién propuesto. */
    public static MiniObjetivo miniObjetivo() {
        return MiniObjetivo.builder()
                .id(MINI_ID)
                .objetivoId(OBJETIVO_ID)
                .negocioId(NEGOCIO_ID)
                .posicion(1)
                .tipo(TipoMiniObjetivo.SUBIR_PRECIO)
                .registroId(PRODUCTO_ID)
                .referencia(PRODUCTO)
                .meta(META_MINI)
                .valorInicial(VALOR_INICIAL)
                .valorActual(VALOR_INICIAL)
                .aporteEstimado(APORTE)
                .estado(EstadoMiniObjetivo.EN_CURSO)
                .semanaInicio(SEMANA)
                .titulo(TITULO)
                .justificacion(JUSTIFICACION)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    public static CandidatoMiniObjetivo candidato() {
        return new CandidatoMiniObjetivo(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO, VALOR_INICIAL,
                META_MINI, APORTE, TITULO, JUSTIFICACION, false);
    }

    /** Un candidato sin producto (de los del negocio, como mejorar el margen). */
    public static CandidatoMiniObjetivo candidatoDelNegocio(TipoMiniObjetivo tipo, String aporte) {
        return new CandidatoMiniObjetivo(tipo, null, null, new BigDecimal("35.71"), new BigDecimal("37.71"),
                new BigDecimal(aporte), "Ganar 2 puntos más", "Hoy te quedan $35,7 de cada $100.", false);
    }
}
