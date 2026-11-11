package com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio;

import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerComoVieneElNegocioTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    /** En UTC ya es el 20 de octubre, pero en Argentina todavía es el 19: el período va del 20/9 al 19/10. */
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");
    private static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);

    private final ObtenerComoVieneElNegocio obtenerComoVieneElNegocio = new ObtenerComoVieneElNegocio(
            negocioRepository, sistemaRepository, datosDeProyectoService, Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Suma los sistemas del negocio de la sesión en los 30 días que terminan hoy, en hora argentina")
    void executeShouldAddEverySystemOfTheBusinessInArgentineTime() {
        givenSistemas(
                List.of(venta(2, "2026-10-19"), venta(9, "2026-10-20")),
                List.of(venta(1, "2026-09-20"), venta(9, "2026-09-19")));

        ObtenerComoVieneElNegocioResponse response = whenObtener();

        assertThat(response.hayVentas()).isTrue();
        assertThat(response.vendiste()).isEqualTo(10200);
        assertThat(response.mercaderia()).isEqualTo(8100);
        assertThat(response.queda()).isEqualTo(2100);
        assertThat(response.semanas()).hasSize(6);
        assertThat(response.semanas().getLast()).isEqualTo(new ObtenerComoVieneElNegocioResponse.Semana("Hoy", 2100));
    }

    @Test
    @DisplayName("Un negocio sin sistemas responde sin ventas y sin semanas")
    void executeShouldReturnNoSalesWhenTheBusinessHasNoSystems() {
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());

        ObtenerComoVieneElNegocioResponse response = whenObtener();

        assertThat(response.hayVentas()).isFalse();
        assertThat(response.vendiste()).isZero();
        assertThat(response.semanas()).isEmpty();
    }

    // --- arrange ---
    @SafeVarargs
    private void givenSistemas(List<Registro>... ventasPorSistema) {
        List<Sistema> sistemas = new ArrayList<>();
        for (List<Registro> ventas : ventasPorSistema) {
            Sistema sistema = mock(Sistema.class);
            sistemas.add(sistema);
            BloqueVentas bloque = new BloqueVentas(AvisoTestData.bloqueVentas(),
                    AvisoTestData.camposPorClave(AvisoTestData.bloqueVentas()), ventas);
            when(datosDeProyectoService.cargar(sistema)).thenReturn(new DatosDeProyecto(
                    "Sistema " + sistemas.size(), List.of(AvisoTestData.inventarioConPrecios(yerba())),
                    List.of(bloque), List.of()));
        }
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(sistemas);
    }

    // --- act ---
    private ObtenerComoVieneElNegocioResponse whenObtener() {
        return obtenerComoVieneElNegocio.execute(new ObtenerComoVieneElNegocioRequest(USUARIO_ID));
    }

    // --- helpers ---
    private static Registro yerba() {
        return AvisoTestData.producto(YERBA, "Yerba Nobleza Gaucha", 40, 3400, 2700);
    }

    private static Registro venta(int cantidad, String fecha) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(AvisoTestData.CAMPO_PRODUCTO_ID.toString(), YERBA.toString());
        valores.put(AvisoTestData.CAMPO_VENTA_CANTIDAD_ID.toString(), cantidad);
        valores.put(AvisoTestData.CAMPO_VENTA_PRECIO_ID.toString(), 3400);
        valores.put(AvisoTestData.CAMPO_FECHA_ID.toString(), fecha);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.VENTAS_ID).valores(valores).build();
    }
}
