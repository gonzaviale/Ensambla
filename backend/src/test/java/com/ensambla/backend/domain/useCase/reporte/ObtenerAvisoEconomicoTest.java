package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico.ObtenerAvisoEconomico;
import com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico.ObtenerAvisoEconomicoRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico.ObtenerAvisoEconomicoResponse;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerAvisoEconomicoTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    /** En UTC ya es el 20, pero en Argentina todavía es el 19: la semana va del 13 al 19 y la anterior del 6 al 12. */
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");
    private static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);

    private final ObtenerAvisoEconomico obtenerAvisoEconomico = new ObtenerAvisoEconomico(
            negocioRepository, proyectoRepository, datosDeProyectoService, Clock.fixed(INSTANTE, ZoneOffset.UTC));

    private Proyecto proyecto;

    @BeforeEach
    void setUp() {
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build();
        proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).nombre("Almacén de prueba").build();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
    }

    @Test
    @DisplayName("Compara los últimos 7 días, en hora argentina, con los 7 anteriores")
    void executeShouldCompareThisWeekWithThePreviousOne() {
        givenVentas(
                venta(3, 3400, "2026-10-19"),
                venta(1, 3400, "2026-10-13"),
                venta(2, 3400, "2026-10-12"),
                venta(1, 3400, "2026-10-06"),
                venta(9, 3400, "2026-10-05"),
                venta(9, 3400, "2026-10-20"));

        ObtenerAvisoEconomicoResponse response = whenObtener();

        assertThat(response.aviso().prioridad()).isEqualTo("BAJA");
        assertThat(response.aviso().tendencia()).isEqualTo("MEJORA");
        assertThat(response.aviso().categoria()).isEqualTo("Rentabilidad");
        assertThat(response.aviso().titulo()).isEqualTo("Las ventas subieron 33 % y la ganancia subió 33 %");
        assertThat(response.aviso().explicacion()).startsWith("Tu negocio está mejorando");
        assertThat(response.aviso().variacionVentas()).isEqualByComparingTo("33");
        assertThat(response.aviso().variacionResultadoBruto()).isEqualByComparingTo("33");
        assertThat(response.aviso().productos()).containsExactly("Yerba Nobleza Gaucha");
        assertThat(response.aviso().confianza()).isEqualTo("ALTA");
    }

    @Test
    @DisplayName("No devuelve aviso si la semana anterior no tuvo ventas")
    void executeShouldReturnNoNoticeWhenThePreviousWeekHasNoSales() {
        givenVentas(venta(3, 3400, "2026-10-19"));

        ObtenerAvisoEconomicoResponse response = whenObtener();

        assertThat(response.aviso()).isNull();
    }

    // --- arrange ---
    private void givenVentas(Registro... ventas) {
        BloqueVentas bloque = new BloqueVentas(AvisoTestData.bloqueVentas(),
                AvisoTestData.camposPorClave(AvisoTestData.bloqueVentas()), List.of(ventas));
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(new DatosDeProyecto(
                "Almacén de prueba", List.of(AvisoTestData.inventarioConPrecios(yerba())), List.of(bloque), List.of()));
    }

    // --- act ---
    private ObtenerAvisoEconomicoResponse whenObtener() {
        return obtenerAvisoEconomico.execute(new ObtenerAvisoEconomicoRequest(USUARIO_ID, PROYECTO_ID));
    }

    // --- helpers ---
    private static Registro yerba() {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(AvisoTestData.CAMPO_NOMBRE_ID.toString(), "Yerba Nobleza Gaucha");
        valores.put(AvisoTestData.CAMPO_CANTIDAD_ID.toString(), 40);
        valores.put(AvisoTestData.CAMPO_PRECIO_ID.toString(), 3400);
        valores.put(AvisoTestData.CAMPO_COSTO_ID.toString(), 2700);
        return Registro.builder().id(RegistroId.de(YERBA)).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.INVENTARIO_ID).valores(valores).build();
    }

    private static Registro venta(int cantidad, int precio, String fecha) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(AvisoTestData.CAMPO_PRODUCTO_ID.toString(), YERBA.toString());
        valores.put(AvisoTestData.CAMPO_VENTA_CANTIDAD_ID.toString(), cantidad);
        valores.put(AvisoTestData.CAMPO_VENTA_PRECIO_ID.toString(), precio);
        valores.put(AvisoTestData.CAMPO_FECHA_ID.toString(), fecha);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.VENTAS_ID).valores(valores).build();
    }
}
