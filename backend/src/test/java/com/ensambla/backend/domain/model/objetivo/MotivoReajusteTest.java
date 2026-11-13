package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MotivoReajusteTest {

    @Test
    @DisplayName("Lo logrado sube la meta, la decisión sin efecto prueba otro camino y lo demás se reemplaza")
    void tipoShouldMatchEachMotivo() {
        assertThat(MotivoReajuste.LOGRADO.tipo()).isEqualTo(TipoReajuste.SUBIR_META);
        assertThat(MotivoReajuste.DECISION_SIN_EFECTO.tipo()).isEqualTo(TipoReajuste.PROBAR_OTRO_CAMINO);
        assertThat(MotivoReajuste.QUITADO.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(MotivoReajuste.PRODUCTO_FUERA.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(MotivoReajuste.SIN_AVANCE.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
    }
}
