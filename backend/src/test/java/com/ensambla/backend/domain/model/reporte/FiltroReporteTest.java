package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiltroReporteTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID ESTADO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID PAGADO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000002");
    private static final UUID NOMBRE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000003");

    @Test
    @DisplayName("Sin filtros no acota nada")
    void armarShouldNotLimitAnythingWithoutFilters() {
        FiltroReporte result = armar(null, null, null, null, null);

        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(result.texto()).isNull();
        assertThat(result.desde()).isNull();
        assertThat(result.hasta()).isNull();
        assertThat(result.valor()).isNull();
        assertThat(result.filtraPorColumna()).isFalse();
    }

    @Test
    @DisplayName("Las fechas incluyen los dos días enteros en la zona de Argentina")
    void armarShouldIncludeBothDaysInArgentinaTime() {
        FiltroReporte result = armar(" pan ", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"), null, null);

        assertThat(result.texto()).isEqualTo("pan");
        assertThat(result.desde()).isEqualTo(Instant.parse("2026-10-01T03:00:00Z"));
        assertThat(result.hasta()).isEqualTo(Instant.parse("2026-11-01T03:00:00Z"));
    }

    @Test
    @DisplayName("Un texto en blanco no filtra")
    void armarShouldIgnoreBlankText() {
        assertThat(armar("   ", null, null, null, null).texto()).isNull();
    }

    @Test
    @DisplayName("Acepta un solo extremo del rango y el mismo día en los dos")
    void armarShouldAcceptOneEndAndTheSameDay() {
        LocalDate dia = LocalDate.parse("2026-10-13");

        assertThat(armar(null, dia, null, null, null).hasta()).isNull();
        assertThat(armar(null, null, dia, null, null).desde()).isNull();
        FiltroReporte mismoDia = armar(null, dia, dia, null, null);
        assertThat(mismoDia.hasta()).isEqualTo(Instant.parse("2026-10-14T03:00:00Z"));
    }

    @Test
    @DisplayName("Rechaza una fecha desde posterior a la fecha hasta")
    void armarShouldRejectReversedDates() {
        assertThatThrownBy(() -> armar(null, LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-01"), null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La fecha desde no puede ser posterior a la fecha hasta");
    }

    @Test
    @DisplayName("Filtra por un valor de una columna de opción o de sí/no")
    void armarShouldFilterByOptionAndYesNoColumns() {
        FiltroReporte opcion = armar(null, null, null, ESTADO_ID, "Entregado");
        FiltroReporte siNo = armar(null, null, null, PAGADO_ID, "true");

        assertThat(opcion.filtraPorColumna()).isTrue();
        assertThat(opcion.campoId()).isEqualTo(ESTADO_ID);
        assertThat(opcion.valor()).isEqualTo("Entregado");
        assertThat(siNo.valor()).isEqualTo("true");
        assertThat(armar(null, null, null, PAGADO_ID, "false").valor()).isEqualTo("false");
    }

    @Test
    @DisplayName("Rechaza filtrar por una columna que no es de opción ni de sí/no, o que no existe")
    void armarShouldRejectColumnsThatCannotBeFiltered() {
        assertThatThrownBy(() -> armar(null, null, null, NOMBRE_ID, "x"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede filtrar por ese campo");
        assertThatThrownBy(() -> armar(null, null, null, UUID.randomUUID(), "x"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede filtrar por ese campo");
    }

    @Test
    @DisplayName("Pide el valor y rechaza uno que la columna no admite")
    void armarShouldRequireAValidValue() {
        assertThatThrownBy(() -> armar(null, null, null, ESTADO_ID, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Elegí el valor por el que filtrar");
        assertThatThrownBy(() -> armar(null, null, null, ESTADO_ID, " "))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> armar(null, null, null, ESTADO_ID, "Perdido"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Elegí un valor de la columna «Estado»");
        assertThatThrownBy(() -> armar(null, null, null, PAGADO_ID, "quizás"))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static FiltroReporte armar(String texto, LocalDate desde, LocalDate hasta, UUID campoId, String valor) {
        List<Campo> campos = List.of(
                Campo.builder().id(ESTADO_ID).nombre("Estado").tipo(TipoCampo.OPCION)
                        .opciones(List.of("Pendiente", "Entregado")).build(),
                Campo.builder().id(PAGADO_ID).nombre("Pagado").tipo(TipoCampo.SI_NO).build(),
                Campo.builder().id(NOMBRE_ID).nombre("Nombre").tipo(TipoCampo.TEXTO).build());
        return FiltroReporte.armar(NEGOCIO_ID, BLOQUE_ID, campos, texto, desde, hasta, campoId, valor);
    }
}
