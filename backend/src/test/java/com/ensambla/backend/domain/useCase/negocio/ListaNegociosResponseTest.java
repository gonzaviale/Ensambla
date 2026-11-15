package com.ensambla.backend.domain.useCase.negocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListaNegociosResponseTest {

    private static final NegocioId PRIMERO = NegocioId.de("10000000-0000-4000-8000-000000000001");
    private static final NegocioId SEGUNDO = NegocioId.de("10000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime FECHA = OffsetDateTime.parse("2026-11-13T12:00:00Z");

    @Test
    @DisplayName("Marca uno solo como activo y permite crear mientras quede cupo")
    void deShouldMarkActiveAndAllowCreationBelowLimit() {
        ListaNegociosResponse response = ListaNegociosResponse.de(
                List.of(negocio(PRIMERO, "Primero"), negocio(SEGUNDO, "Segundo")), SEGUNDO, 6);

        assertThat(response.activoId()).isEqualTo(SEGUNDO);
        assertThat(response.puedeCrear()).isTrue();
        assertThat(response.negocios()).extracting(NegocioDeLaCuentaItem::activo)
                .containsExactly(false, true);
        assertThat(response.negocios().getFirst()).extracting(
                NegocioDeLaCuentaItem::nombre,
                NegocioDeLaCuentaItem::rubro,
                NegocioDeLaCuentaItem::creadoEn)
                .containsExactly("Primero", "KIOSCO", FECHA);
    }

    @Test
    @DisplayName("Con la cantidad igual al límite ya no permite crear")
    void deShouldRejectCreationAtLimit() {
        ListaNegociosResponse response = ListaNegociosResponse.de(List.of(negocio(PRIMERO, "Único")), PRIMERO, 1);

        assertThat(response.puedeCrear()).isFalse();
    }

    private Negocio negocio(NegocioId id, String nombre) {
        return Negocio.builder().id(id).nombre(nombre).rubro(RubroNegocio.KIOSCO).creadoEn(FECHA).build();
    }
}
