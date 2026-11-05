package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.bloque.Bloque;
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
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicio;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioResponse;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.when;

class ObtenerResumenInicioTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000002");
    /** En UTC ya es el 20, pero en Argentina todavía es el 19. */
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID FIDEOS = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID MERMELADA = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final UUID ACEITE = UUID.fromString("d0000000-0000-4000-8000-000000000004");
    private static final UUID ARROZ = UUID.fromString("d0000000-0000-4000-8000-000000000005");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);

    private final ObtenerResumenInicio obtenerResumenInicio = new ObtenerResumenInicio(
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
    @DisplayName("Suma lo vendido hoy, en hora argentina, y lo compara con el mismo día de la semana pasada")
    void executeShouldSumTodaysSalesAndCompareWithLastWeek() {
        givenDatos(
                List.of(
                        venta(YERBA, 2, 3400, "2026-10-19"),
                        venta(YERBA, 1, 3400, "2026-10-19"),
                        venta(FIDEOS, 3, null, "2026-10-19"),
                        venta(YERBA, 1, 3400, "2026-10-12"),
                        venta(YERBA, 9, 3400, "2026-10-20")),
                yerba());

        ObtenerResumenInicioResponse response = whenObtener();

        assertThat(response.sistema()).isEqualTo("Almacén de prueba");
        assertThat(response.hoy()).isEqualTo(HOY);
        assertThat(response.hayVentas()).isTrue();
        assertThat(response.ventasDeHoy().ingresos()).isEqualByComparingTo("10200");
        assertThat(response.ventasDeHoy().unidades()).isEqualByComparingTo("6");
        assertThat(response.ventasDeHoy().cantidad()).isEqualTo(3);
        assertThat(response.ventasDeHoy().sinPrecio()).isEqualTo(1);
        assertThat(response.ventasDeHoy().ingresosSemanaPasada()).isEqualByComparingTo("3400");
        assertThat(response.ultimoDiaConVentas().fecha()).isEqualTo(HOY);
        assertThat(response.ultimoDiaConVentas().ingresos()).isEqualByComparingTo("10200");
    }

    @Test
    @DisplayName("Sin ventas hoy devuelve cero y el último día en que se anotó una venta")
    void executeShouldReturnTheLastDayWithSalesWhenThereAreNoneToday() {
        givenDatos(
                List.of(
                        venta(YERBA, 2, 3400, "2026-10-16"),
                        venta(YERBA, 1, 3400, "2026-10-10")),
                yerba());

        ObtenerResumenInicioResponse response = whenObtener();

        assertThat(response.ventasDeHoy().ingresos()).isEqualByComparingTo("0");
        assertThat(response.ventasDeHoy().cantidad()).isZero();
        assertThat(response.ultimoDiaConVentas().fecha()).isEqualTo(LocalDate.of(2026, 10, 16));
        assertThat(response.ultimoDiaConVentas().ingresos()).isEqualByComparingTo("6800");
    }

    @Test
    @DisplayName("Un sistema sin ventas ni productos no tiene nada para mostrar")
    void executeShouldReturnEmptyMetricsWhenThereIsNothingLoaded() {
        givenDatos(List.of());

        ObtenerResumenInicioResponse response = whenObtener();

        assertThat(response.hayVentas()).isFalse();
        assertThat(response.hayProductos()).isFalse();
        assertThat(response.ultimoDiaConVentas()).isNull();
        assertThat(response.masVendido()).isNull();
        assertThat(response.faltantes().total()).isZero();
        assertThat(response.faltantes().primeros()).isEmpty();
        assertThat(response.parados()).isEmpty();
    }

    @Test
    @DisplayName("Devuelve el producto que más salió en los últimos 30 días con su ganancia")
    void executeShouldReturnTheBestSellerOfTheLastThirtyDays() {
        givenDatos(
                List.of(
                        venta(YERBA, 5, 3400, "2026-10-10"),
                        venta(FIDEOS, 9, 1700, "2026-10-11"),
                        venta(MERMELADA, 50, 2600, "2026-08-01")),
                yerba(), fideos(), mermelada());

        ObtenerResumenInicioResponse response = whenObtener();

        assertThat(response.hayProductos()).isTrue();
        assertThat(response.masVendido().nombre()).isEqualTo("Fideos Luchetti");
        assertThat(response.masVendido().unidades()).isEqualByComparingTo("9");
        assertThat(response.masVendido().ingresos()).isEqualByComparingTo("15300");
        assertThat(response.masVendido().ganancia()).isNull();
    }

    @Test
    @DisplayName("Avisa los tres productos más urgentes y cuenta todos los que faltan")
    void executeShouldListTheThreeMostUrgentMissingProducts() {
        givenDatos(
                List.of(),
                producto(YERBA, "Yerba", 0, 3400, 2700, 10),
                producto(FIDEOS, "Fideos", 1, 1700, null, 6),
                producto(MERMELADA, "Mermelada", 2, 2600, 1800, 4),
                producto(ACEITE, "Aceite", 3, 2800, null, 6),
                producto(ARROZ, "Arroz", 50, 900, null, 6));

        ObtenerResumenInicioResponse response = whenObtener();

        assertThat(response.faltantes().total()).isEqualTo(4);
        assertThat(response.faltantes().primeros()).hasSize(3);
        assertThat(response.faltantes().primeros().getFirst().nombre()).isEqualTo("Yerba");
        assertThat(response.faltantes().primeros().getFirst().minimo()).isEqualByComparingTo("10");
        assertThat(response.faltantes().primeros().getFirst().pedir()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Lista los tres productos parados con más plata invertida cuando ya hay dos semanas de ventas")
    void executeShouldListTheStoppedProductsWhenThereAreTwoWeeksOfSales() {
        givenDatos(
                List.of(
                        venta(ARROZ, 1, 900, "2026-09-01"),
                        venta(ARROZ, 1, 900, "2026-10-18")),
                producto(YERBA, "Yerba", 40, 3400, 2700, null),
                producto(MERMELADA, "Mermelada", 20, 2600, 1800, null),
                producto(ACEITE, "Aceite", 3, 2800, null, null),
                producto(FIDEOS, "Fideos", 8, 1700, 1000, null),
                producto(ARROZ, "Arroz", 50, 900, 600, null));

        ObtenerResumenInicioResponse response = whenObtener();

        assertThat(response.parados()).hasSize(3);
        assertThat(response.parados().get(0).nombre()).isEqualTo("Yerba");
        assertThat(response.parados().get(0).capital()).isEqualByComparingTo("108000");
        assertThat(response.parados().get(1).nombre()).isEqualTo("Mermelada");
        assertThat(response.parados().get(2).nombre()).isEqualTo("Fideos");
    }

    @Test
    @DisplayName("No dice que un producto está parado si el sistema todavía no tiene dos semanas de ventas")
    void executeShouldNotListStoppedProductsWithLessThanTwoWeeksOfSales() {
        givenDatos(
                List.of(venta(ARROZ, 1, 900, "2026-10-15")),
                producto(YERBA, "Yerba", 40, 3400, 2700, null),
                producto(ARROZ, "Arroz", 50, 900, 600, null));

        ObtenerResumenInicioResponse response = whenObtener();

        assertThat(response.parados()).isEmpty();
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void executeShouldNotFindForeignProject() {
        when(proyectoRepository.obtener(NEGOCIO_ID, OTRO_PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> obtenerResumenInicio.execute(
                new ObtenerResumenInicioRequest(USUARIO_ID, OTRO_PROYECTO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenDatos(List<Registro> ventas, Registro... productos) {
        Bloque bloqueDeVentas = AvisoTestData.bloqueVentas();
        BloqueVentas ventasDelSistema = new BloqueVentas(
                bloqueDeVentas, AvisoTestData.camposPorClave(bloqueDeVentas), ventas);
        List<BloqueInventario> inventarios = productos.length == 0
                ? List.of()
                : List.of(AvisoTestData.inventarioConPrecios(productos));
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(new DatosDeProyecto(
                "Almacén de prueba", inventarios, List.of(ventasDelSistema), List.of()));
    }

    // --- act ---
    private ObtenerResumenInicioResponse whenObtener() {
        return obtenerResumenInicio.execute(new ObtenerResumenInicioRequest(USUARIO_ID, PROYECTO_ID));
    }

    // --- helpers ---
    private static Registro yerba() {
        return producto(YERBA, "Yerba Nobleza Gaucha", 40, 3400, 2700, 10);
    }

    private static Registro fideos() {
        return producto(FIDEOS, "Fideos Luchetti", 10, 1700, null, 6);
    }

    private static Registro mermelada() {
        return producto(MERMELADA, "Mermelada BC", 20, 2600, 1800, 4);
    }

    private static Registro producto(UUID id, String nombre, Object cantidad, Object precio, Object costo,
                                     Object minimo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, AvisoTestData.CAMPO_NOMBRE_ID.toString(), nombre);
        poner(valores, AvisoTestData.CAMPO_CANTIDAD_ID.toString(), cantidad);
        poner(valores, AvisoTestData.CAMPO_PRECIO_ID.toString(), precio);
        poner(valores, AvisoTestData.CAMPO_COSTO_ID.toString(), costo);
        poner(valores, AvisoTestData.CAMPO_MINIMO_ID.toString(), minimo);
        return Registro.builder().id(RegistroId.de(id)).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.INVENTARIO_ID).valores(valores).build();
    }

    private static Registro venta(UUID producto, int cantidad, Integer precio, String fecha) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, AvisoTestData.CAMPO_PRODUCTO_ID.toString(), producto.toString());
        poner(valores, AvisoTestData.CAMPO_VENTA_CANTIDAD_ID.toString(), cantidad);
        poner(valores, AvisoTestData.CAMPO_VENTA_PRECIO_ID.toString(), precio);
        poner(valores, AvisoTestData.CAMPO_FECHA_ID.toString(), fecha);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.VENTAS_ID).valores(valores).build();
    }

    private static void poner(Map<String, Object> valores, String campoId, Object valor) {
        if (valor != null) {
            valores.put(campoId, valor);
        }
    }
}
