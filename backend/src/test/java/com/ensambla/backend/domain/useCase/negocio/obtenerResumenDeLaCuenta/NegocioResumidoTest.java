package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.negocio.EstadoDelNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.objetivo.ComoVieneElNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NegocioResumidoTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final String NOMBRE = "Kiosco de la esquina";
    private static final long VENDISTE = 280000;
    private static final long QUEDA = -72000;
    private static final long AVISOS_PENDIENTES = 2;

    @Test
    @DisplayName("Copia los datos del negocio, lo vendido como ventas, lo que queda como margen y su estado")
    void deShouldCopyAllFields() {
        NegocioResumido result = NegocioResumido.de(negocio(), comoViene(), AVISOS_PENDIENTES);

        assertThat(result.id()).isEqualTo(NEGOCIO_ID);
        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.rubro()).isEqualTo(RubroNegocio.KIOSCO);
        assertThat(result.ventas()).isEqualTo(VENDISTE);
        assertThat(result.margen()).isEqualTo(QUEDA);
        assertThat(result.avisosPendientes()).isEqualTo(AVISOS_PENDIENTES);
        assertThat(result.estado()).isEqualTo(EstadoDelNegocio.ATENCION);
    }

    // --- helpers ---
    private static Negocio negocio() {
        return Negocio.builder()
                .id(NEGOCIO_ID)
                .nombre(NOMBRE)
                .rubro(RubroNegocio.KIOSCO)
                .build();
    }

    private static ComoVieneElNegocio comoViene() {
        return new ComoVieneElNegocio(true, VENDISTE, 300000, 52000, QUEDA, 0, List.of());
    }
}
