package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @DisplayName("Resume cada negocio del dueño con las métricas del panel de los 30 días que terminan hoy, en hora "
            + "argentina")
    void executeShouldSummarizeEveryBusinessOfTheOwnerInArgentineTime() {
        givenNegocios();
        givenVentas(ALMACEN_ID, venta(2, "2026-10-19"), venta(9, "2026-10-20"), venta(9, "2026-09-19"));
        givenVentas(KIOSCO_ID);
        givenPendientes(Map.of(ALMACEN_ID, 3L));

        ObtenerResumenDeLaCuentaResponse response = whenObtener(null);

        assertThat(response.desde()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(response.hasta()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(response.dias()).isEqualTo(30);
        assertThat(response.negocios()).extracting(NegocioResumido::id).containsExactly(ALMACEN_ID, KIOSCO_ID);
        thenAlmacenVendioDosYerbas(response.negocios().getFirst());
    }

    @Test
    @DisplayName("Marca como activo sólo al negocio con el que trabaja la sesión")
    void executeShouldMarkOnlyTheSessionBusinessAsActive() {
        givenNegocios();
        givenVentas(ALMACEN_ID);
        givenVentas(KIOSCO_ID);
        givenPendientes(Map.of());

        ObtenerResumenDeLaCuentaResponse response = whenObtener(null);

        assertThat(response.negocios()).extracting(NegocioResumido::activo).containsExactly(true, false);
    }

    @Test
    @DisplayName("Un negocio sin ventas en el período no tiene datos, pero conserva sus avisos pendientes")
    void executeShouldReturnNoDataForABusinessWithoutSales() {
        givenNegocios();
        givenVentas(ALMACEN_ID, venta(1, "2026-10-19"));
        givenVentas(KIOSCO_ID);
        givenPendientes(Map.of(KIOSCO_ID, 2L));

        NegocioResumido kiosco = whenObtener(null).negocios().getLast();

        assertThat(kiosco.hayDatos()).isFalse();
        assertThat(kiosco.vendiste()).isNull();
        assertThat(kiosco.queda()).isNull();
        assertThat(kiosco.avisosPendientes()).isEqualTo(2);
    }

    @Test
    @DisplayName("Mira sólo los días pedidos")
    void executeShouldOnlyLookAtTheRequestedDays() {
        givenNegocios();
        givenVentas(ALMACEN_ID, venta(1, "2026-10-19"), venta(5, "2026-10-12"));
        givenVentas(KIOSCO_ID);
        givenPendientes(Map.of());

        ObtenerResumenDeLaCuentaResponse response = whenObtener(7);

        assertThat(response.desde()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(response.dias()).isEqualTo(7);
        assertThat(response.negocios().getFirst().vendiste()).isEqualByComparingTo("3400");
    }

    @Test
    @DisplayName("Una cantidad de días fuera de rango se rechaza sin cargar ningún negocio")
    void executeShouldRejectDaysOutOfRangeWithoutLoadingAnything() {
        assertThatThrownBy(() -> whenObtener(367)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> whenObtener(0)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(negocioRepository, sistemaRepository, avisoRepository);
    }

    @Test
    @DisplayName("Cuenta de una vez los avisos pendientes de todos los negocios del dueño")
    void executeShouldCountThePendingAvisosOfEveryBusinessAtOnce() {
        givenNegocios();
        givenVentas(ALMACEN_ID);
        givenVentas(KIOSCO_ID);
        givenPendientes(Map.of(ALMACEN_ID, 3L, KIOSCO_ID, 2L));

        ObtenerResumenDeLaCuentaResponse response = whenObtener(null);

        verify(avisoRepository).contarPorNegocio(List.of(ALMACEN_ID, KIOSCO_ID), EstadoAviso.PENDIENTE);
        assertThat(response.total().avisosPendientes()).isEqualTo(5);
    }

    @Test
    @DisplayName("Suma en el total lo de todos los negocios")
    void executeShouldAddUpTheTotalOfEveryBusiness() {
        givenNegocios();
        givenVentas(ALMACEN_ID, venta(2, "2026-10-19"));
        givenVentas(KIOSCO_ID, venta(1, "2026-10-18"));
        givenPendientes(Map.of());

        ObtenerResumenDeLaCuentaResponse.Total total = whenObtener(null).total();

        assertThat(total.negocios()).isEqualTo(2);
        assertThat(total.vendiste()).isEqualByComparingTo("10200");
        assertThat(total.queda()).isEqualByComparingTo("10200");
        assertThat(total.avisosPendientes()).isZero();
    }

    // --- arrange ---
    private void givenNegocios() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(ALMACEN_ID);
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
    private ObtenerResumenDeLaCuentaResponse whenObtener(Integer dias) {
        return obtenerResumenDeLaCuenta.execute(new ObtenerResumenDeLaCuentaRequest(USUARIO_ID, dias));
    }

    // --- assert ---
    /** Sin tabla de gastos ni de entradas lo que queda es todo lo cobrado, y los gastos no se conocen. */
    private static void thenAlmacenVendioDosYerbas(NegocioResumido almacen) {
        assertThat(almacen.nombre()).isEqualTo("Almacén Don Pepe");
        assertThat(almacen.rubro()).isEqualTo(RubroNegocio.ALMACEN);
        assertThat(almacen.hayDatos()).isTrue();
        assertThat(almacen.vendiste()).isEqualByComparingTo("6800");
        assertThat(almacen.gastos()).isNull();
        assertThat(almacen.queda()).isEqualByComparingTo("6800");
        assertThat(almacen.teDeben()).isZero();
        assertThat(almacen.avisosPendientes()).isEqualTo(3);
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
