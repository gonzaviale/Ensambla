package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricaTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-12T12:00:00-03:00");

    @Test
    @DisplayName("Crea la métrica con su receta, su lugar y quién la eligió")
    void nuevaShouldKeepAllTheData() {
        DefinicionMetrica definicion = definicion("Ventas del mes");

        Metrica metrica = Metrica.nueva(NEGOCIO_ID, PROYECTO_ID, definicion, 2, AutoriaMetrica.SUGERIDA, AHORA);

        assertThat(metrica.getId()).isNull();
        assertThat(metrica.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(metrica.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(metrica.getDefinicion()).isEqualTo(definicion);
        assertThat(metrica.getOrden()).isEqualTo(2);
        assertThat(metrica.getAutoria()).isEqualTo(AutoriaMetrica.SUGERIDA);
        assertThat(metrica.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("No crea una métrica con una receta inválida")
    void nuevaShouldRejectInvalidDefinition() {
        DefinicionMetrica invalida = definicion(" ");

        assertThatThrownBy(() -> Metrica.nueva(NEGOCIO_ID, PROYECTO_ID, invalida, 0, AutoriaMetrica.PROPIA, AHORA))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static DefinicionMetrica definicion(String nombre) {
        return new DefinicionMetrica(nombre, FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA,
                PeriodoMetrica.MES, false);
    }
}
