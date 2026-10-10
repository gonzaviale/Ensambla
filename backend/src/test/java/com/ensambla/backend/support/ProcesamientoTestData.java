package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.enums.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProcesamientoTestData {

    public static final UUID NEGOCIO_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID LOTE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID BLOQUE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID CAMPO_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private ProcesamientoTestData() {
    }

    public static LoteTrabajo lote(int intentos) {
        return new LoteTrabajo(LOTE_ID, NEGOCIO_ID, null, UUID.randomUUID(), intentos);
    }

    public static PoliticaProcesamiento politica() {
        return new PoliticaProcesamiento(3, 10, 100, Duration.ofMinutes(2), Duration.ofSeconds(30), null, null);
    }

    public static LecturaCuaderno lectura() {
        return lectura(TipoCampo.NUMERO, "100", List.of());
    }

    public static LecturaCuaderno lectura(TipoCampo tipo, String valor, List<String> opciones) {
        return new LecturaCuaderno(TipoRegistroCuaderno.VENTAS,
                new BloquePropuesto("Ventas", null, List.of(new CampoPropuesto("monto", "Monto", tipo, opciones))),
                List.of(new FilaLeida(1, 2, Map.of("monto", new DatoLeido(valor, 0.9)))),
                List.of("Aviso"), new ConsumoIa("simulado", "demo", "v1", 1, 10, 20, 30));
    }

    public static PropuestaLectura propuesta() {
        return new PropuestaLectura(lectura(), Map.of("monto", CAMPO_ID));
    }
}
