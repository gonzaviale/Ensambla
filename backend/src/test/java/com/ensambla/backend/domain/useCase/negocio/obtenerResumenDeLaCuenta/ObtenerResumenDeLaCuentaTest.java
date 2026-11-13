package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.negocio.EstadoDelNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObtenerResumenDeLaCuentaTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId ALMACEN_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final NegocioId KIOSCO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000002");
    /** En UTC ya es el 20 de octubre, pero en Argentina todavía es el 19: el período va del 20/9 al 19/10. */
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");
    private static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final SistemaRepository sistemaRepository = mock(SistemaRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);

    private final ObtenerResumenDeLaCuenta obtenerResumenDeLaCuenta = new ObtenerResumenDeLaCuenta(
            negocioRepository, sistemaRepository, datosDeProyectoService, avisoRepository,
            Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @Test
    @DisplayName("Resume cada negocio del dueño con sus ventas de los 30 días que terminan hoy, en hora argentina")
    void executeShouldSummarizeEveryBusinessOfTheOwnerInArgentineTime() {
        givenNegocios();
        givenVentas(ALMACEN_ID, venta(2, "2026-10-19"), venta(9, "2026-10-20"), venta(9, "2026-09-19"));
        givenVentas(KIOSCO_ID);
        givenPendientes(Map.of(ALMACEN_ID, 3L));

        ObtenerResumenDeLaCuentaResponse response = whenObtener();

        assertThat(response.periodo().desde()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(response.periodo().hasta()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(response.negocios()).extracting(NegocioResumido::id).containsExactly(ALMACEN_ID, KIOSCO_ID);
        thenAlmacenVendioDosYerbas(response.negocios().getFirst());
    }

    @Test
    @DisplayName("Un negocio sin ventas ni avisos queda sin datos, con todo en cero")
    void executeShouldReturnSinDatosForABusinessWithoutSalesOrAvisos() {
        givenNegocios();
        givenVentas(ALMACEN_ID, venta(1, "2026-10-19"));
        givenVentas(KIOSCO_ID);
        givenPendientes(Map.of(ALMACEN_ID, 3L));

        NegocioResumido kiosco = whenObtener().negocios().getLast();

        assertThat(kiosco.ventas()).isZero();
        assertThat(kiosco.margen()).isZero();
        assertThat(kiosco.avisosPendientes()).isZero();
        assertThat(kiosco.estado()).isEqualTo(EstadoDelNegocio.SIN_DATOS);
    }

    @Test
    @DisplayName("Cuenta de una vez los avisos pendientes de todos los negocios del dueño")
    void executeShouldCountThePendingAvisosOfEveryBusinessAtOnce() {
        givenNegocios();
        givenVentas(ALMACEN_ID);
        givenVentas(KIOSCO_ID);
        givenPendientes(Map.of(ALMACEN_ID, 3L, KIOSCO_ID, 2L));

        ObtenerResumenDeLaCuentaResponse response = whenObtener();

        verify(avisoRepository).contarPorNegocio(List.of(ALMACEN_ID, KIOSCO_ID), EstadoAviso.PENDIENTE);
        assertThat(response.totales().avisosPendientes()).isEqualTo(5);
    }

    @Test
    @DisplayName("Suma en los totales lo de todos los negocios")
    void executeShouldAddUpTheTotalsOfEveryBusiness() {
        givenNegocios();
        givenVentas(ALMACEN_ID, venta(2, "2026-10-19"));
        givenVentas(KIOSCO_ID, venta(1, "2026-10-18"));
        givenPendientes(Map.of());

        ObtenerResumenDeLaCuentaResponse response = whenObtener();

        assertThat(response.totales().ventas()).isEqualTo(10200);
        assertThat(response.totales().margen()).isEqualTo(2100);
        assertThat(response.totales().avisosPendientes()).isZero();
    }

    // --- arrange ---
    private void givenNegocios() {
        when(negocioRepository.findAllByDuenioId(USUARIO_ID)).thenReturn(List.of(
                negocio(ALMACEN_ID, "Almacén Don Pepe", RubroNegocio.ALMACEN),
                negocio(KIOSCO_ID, "Kiosco de la esquina", RubroNegocio.KIOSCO)));
    }

    private void givenVentas(NegocioId negocioId, Registro... ventas) {
        Sistema sistema = mock(Sistema.class);
        BloqueVentas bloque = new BloqueVentas(AvisoTestData.bloqueVentas(),
                AvisoTestData.camposPorClave(AvisoTestData.bloqueVentas()), List.of(ventas));
        when(datosDeProyectoService.cargar(sistema)).thenReturn(new DatosDeProyecto(
                "Ventas", List.of(AvisoTestData.inventarioConPrecios(yerba())), List.of(bloque), List.of()));
        when(sistemaRepository.findAllByNegocioId(negocioId)).thenReturn(List.of(sistema));
    }

    private void givenPendientes(Map<NegocioId, Long> pendientes) {
        when(avisoRepository.contarPorNegocio(List.of(ALMACEN_ID, KIOSCO_ID), EstadoAviso.PENDIENTE))
                .thenReturn(pendientes);
    }

    // --- act ---
    private ObtenerResumenDeLaCuentaResponse whenObtener() {
        return obtenerResumenDeLaCuenta.execute(new ObtenerResumenDeLaCuentaRequest(USUARIO_ID));
    }

    // --- assert ---
    private static void thenAlmacenVendioDosYerbas(NegocioResumido almacen) {
        assertThat(almacen.nombre()).isEqualTo("Almacén Don Pepe");
        assertThat(almacen.rubro()).isEqualTo(RubroNegocio.ALMACEN);
        assertThat(almacen.ventas()).isEqualTo(6800);
        assertThat(almacen.margen()).isEqualTo(1400);
        assertThat(almacen.avisosPendientes()).isEqualTo(3);
        assertThat(almacen.estado()).isEqualTo(EstadoDelNegocio.BIEN);
    }

    // --- helpers ---
    private static Negocio negocio(NegocioId id, String nombre, RubroNegocio rubro) {
        return Negocio.builder()
                .id(id)
                .nombre(nombre)
                .rubro(rubro)
                .build();
    }

    private static Registro yerba() {
        return AvisoTestData.producto(YERBA, "Yerba Nobleza Gaucha", 40, 3400, 2700);
    }

    private static Registro venta(int cantidad, String fecha) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(AvisoTestData.CAMPO_PRODUCTO_ID.toString(), YERBA.toString());
        valores.put(AvisoTestData.CAMPO_VENTA_CANTIDAD_ID.toString(), cantidad);
        valores.put(AvisoTestData.CAMPO_VENTA_PRECIO_ID.toString(), 3400);
        valores.put(AvisoTestData.CAMPO_FECHA_ID.toString(), fecha);
        return Registro.builder()
                .id(RegistroId.nuevo())
                .negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.VENTAS_ID)
                .valores(valores)
                .build();
    }
}
