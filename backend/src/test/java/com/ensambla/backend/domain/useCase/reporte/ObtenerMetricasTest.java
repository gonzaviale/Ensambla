package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoMetricas;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricas;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricasRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricasResponse;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerMetricasTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000002");
    /** En UTC ya es el 20, pero en Argentina todavía es el 19. */
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID MERMELADA = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final CampoId CAMPO_FIADO_ID = CampoId.de("f0000000-0000-4000-8000-000000000016");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final CampoId GASTO_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000022");
    private static final CampoId GASTO_TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");
    private static final CampoId GASTO_CATEGORIA_ID = CampoId.de("f0000000-0000-4000-8000-000000000024");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);

    private final ObtenerMetricas obtenerMetricas = new ObtenerMetricas(
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
    @DisplayName("Arma las métricas de la semana: ventas, día por día, productos, stock, clientes y fiado")
    void executeShouldSummarizeTheWeek() {
        givenDatos(List.of(
                ventaAFiado(YERBA, 2, 3400, "2026-10-19"),
                venta(YERBA, 1, 3400, "2026-10-15"),
                venta(MERMELADA, 3, null, "2026-10-14"),
                venta(YERBA, 1, 3400, "2026-10-12"),
                venta(YERBA, 9, 3400, "2026-10-20")), List.of());

        ObtenerMetricasResponse response = whenObtener(PeriodoMetricas.SEMANA);

        assertThat(response.sistema()).isEqualTo("Almacén de prueba");
        assertThat(response.periodo()).isEqualTo(PeriodoMetricas.SEMANA);
        assertThat(response.desde()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(response.hasta()).isEqualTo(HOY);
        assertThat(response.hayVentas()).isTrue();
        assertThat(response.ventas().ingresos()).isEqualByComparingTo("10200");
        assertThat(response.ventas().unidades()).isEqualByComparingTo("6");
        assertThat(response.ventas().cantidad()).isEqualTo(3);
        assertThat(response.ventas().sinPrecio()).isEqualTo(1);
        assertThat(response.ventas().ingresoPorDia()).isEqualByComparingTo("1457.14");
        assertThat(response.ventas().ingresosAnteriores()).isEqualByComparingTo("3400");
        assertThat(response.ventas().gananciaEstimada()).isEqualByComparingTo("2100");
        assertThat(response.porDia()).hasSize(7);
        assertThat(response.porDia().getLast().ingresos()).isEqualByComparingTo("6800");
        assertThat(response.masVendidos()).extracting(ObtenerMetricasResponse.ProductoVendido::nombre)
                .containsExactly("Yerba", "Mermelada");
        assertThat(response.masVendidos().get(0).ganancia()).isEqualByComparingTo("2100");
        assertThat(response.masVendidos().get(1).ganancia()).isNull();
        assertThat(response.stock().productos()).isEqualTo(2);
        assertThat(response.stock().totalFaltantes()).isEqualTo(1);
        assertThat(response.stock().faltantes().getFirst().nombre()).isEqualTo("Yerba");
        assertThat(response.stock().valorACosto()).isEqualByComparingTo("28800");
        assertThat(response.clientes().fiado()).isEqualByComparingTo("6800");
        assertThat(response.clientes().ventasFiadas()).isEqualTo(1);
        assertThat(response.clientes().top()).singleElement().satisfies(cliente -> {
            assertThat(cliente.nombre()).isEqualTo("Doña Rosa");
            assertThat(cliente.compras()).isEqualTo(1);
            assertThat(cliente.ingresos()).isEqualByComparingTo("6800");
        });
        assertThat(response.gastos()).isNull();
    }

    @Test
    @DisplayName("Hoy se compara con el mismo día de la semana pasada")
    void executeShouldCompareTodayWithTheSameDayLastWeek() {
        givenDatos(List.of(
                venta(YERBA, 2, 3400, "2026-10-19"),
                venta(YERBA, 5, 3400, "2026-10-18"),
                venta(YERBA, 1, 3400, "2026-10-12")), List.of());

        ObtenerMetricasResponse response = whenObtener(PeriodoMetricas.HOY);

        assertThat(response.ventas().ingresos()).isEqualByComparingTo("6800");
        assertThat(response.ventas().ingresosAnteriores()).isEqualByComparingTo("3400");
        assertThat(response.porDia()).hasSize(1);
    }

    @Test
    @DisplayName("Con dos semanas de ventas lista los productos parados; antes no")
    void executeShouldListStoppedProductsOnlyWithTwoWeeksOfSales() {
        givenDatos(List.of(venta(YERBA, 1, 3400, "2026-09-01"), venta(YERBA, 1, 3400, "2026-10-18")), List.of());

        ObtenerMetricasResponse conHistoria = whenObtener(PeriodoMetricas.MES);

        assertThat(conHistoria.parados()).extracting(ObtenerMetricasResponse.Parado::nombre)
                .containsExactly("Mermelada");
        assertThat(conHistoria.parados().getFirst().capital()).isEqualByComparingTo("18000");
    }

    @Test
    @DisplayName("Sin dos semanas de ventas no dice que un producto está parado")
    void executeShouldNotListStoppedProductsWithoutTwoWeeksOfSales() {
        givenDatos(List.of(venta(YERBA, 1, 3400, "2026-10-18")), List.of());

        ObtenerMetricasResponse response = whenObtener(PeriodoMetricas.MES);

        assertThat(response.parados()).isEmpty();
    }

    @Test
    @DisplayName("Con una tabla de gastos suma lo gastado por categoría y el resultado")
    void executeShouldSummarizeExpenses() {
        givenDatos(List.of(venta(YERBA, 2, 3400, "2026-10-19")), List.of(
                gasto("2026-10-18", 1000, "Servicios"),
                gasto("2026-10-17", 500, "Mercadería")));

        ObtenerMetricasResponse response = whenObtener(PeriodoMetricas.SEMANA);

        assertThat(response.gastos().total()).isEqualByComparingTo("1500");
        assertThat(response.gastos().resultado()).isEqualByComparingTo("5300");
        assertThat(response.gastos().porCategoria()).extracting(ObtenerMetricasResponse.GastoPorCategoria::categoria)
                .containsExactly("Servicios", "Mercadería");
        assertThat(response.gastos().porCategoria().getFirst().total()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("Sin ventas anotadas avisa que no hay ventas")
    void executeShouldSayThereAreNoSales() {
        givenDatos(List.of(), List.of());

        ObtenerMetricasResponse response = whenObtener(PeriodoMetricas.SEMANA);

        assertThat(response.hayVentas()).isFalse();
        assertThat(response.masVendidos()).isEmpty();
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void executeShouldNotFindForeignProject() {
        when(proyectoRepository.obtener(NEGOCIO_ID, OTRO_PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> obtenerMetricas.execute(
                new ObtenerMetricasRequest(USUARIO_ID, OTRO_PROYECTO_ID, PeriodoMetricas.SEMANA)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenDatos(List<Registro> ventas, List<Registro> gastos) {
        List<BloqueGastos> tablasDeGastos = gastos.isEmpty() ? List.of() : List.of(bloqueDeGastos(gastos));
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(new DatosDeProyecto(
                "Almacén de prueba",
                List.of(AvisoTestData.inventarioConPrecios(
                        producto(YERBA, "Yerba", 4, 3400, 2700, 10),
                        producto(MERMELADA, "Mermelada", 10, 2600, 1800, 2))),
                List.of(bloqueDeVentas(ventas)),
                tablasDeGastos));
    }

    // --- act ---
    private ObtenerMetricasResponse whenObtener(PeriodoMetricas periodo) {
        return obtenerMetricas.execute(new ObtenerMetricasRequest(USUARIO_ID, PROYECTO_ID, periodo));
    }

    // --- helpers ---
    private static Registro producto(UUID id, String nombre, Object cantidad, Object precio, Object costo,
                                     Object minimo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(AvisoTestData.CAMPO_NOMBRE_ID.toString(), nombre);
        valores.put(AvisoTestData.CAMPO_CANTIDAD_ID.toString(), cantidad);
        valores.put(AvisoTestData.CAMPO_PRECIO_ID.toString(), precio);
        valores.put(AvisoTestData.CAMPO_COSTO_ID.toString(), costo);
        valores.put(AvisoTestData.CAMPO_MINIMO_ID.toString(), minimo);
        return Registro.builder().id(RegistroId.de(id)).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.INVENTARIO_ID).valores(valores).build();
    }

    private static Registro venta(UUID producto, int cantidad, Integer precio, String fecha) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(AvisoTestData.CAMPO_PRODUCTO_ID.toString(), producto.toString());
        valores.put(AvisoTestData.CAMPO_VENTA_CANTIDAD_ID.toString(), cantidad);
        if (precio != null) {
            valores.put(AvisoTestData.CAMPO_VENTA_PRECIO_ID.toString(), precio);
        }
        valores.put(AvisoTestData.CAMPO_FECHA_ID.toString(), fecha);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.VENTAS_ID).valores(valores).build();
    }

    private static Registro ventaAFiado(UUID producto, int cantidad, Integer precio, String fecha) {
        Registro venta = venta(producto, cantidad, precio, fecha);
        venta.getValores().put(CAMPO_FIADO_ID.toString(), true);
        venta.getValores().put(AvisoTestData.CAMPO_CLIENTE_ID.toString(), "Doña Rosa");
        return venta;
    }

    private static BloqueVentas bloqueDeVentas(List<Registro> ventas) {
        Bloque base = AvisoTestData.bloqueVentas();
        List<Campo> campos = new ArrayList<>(base.getCampos());
        campos.add(campo(CAMPO_FIADO_ID, AvisoTestData.VENTAS_ID, "credit"));
        Bloque bloque = Bloque.builder().id(base.getId()).negocioId(base.getNegocioId())
                .proyectoId(base.getProyectoId()).tipo(base.getTipo()).campos(campos).build();
        return new BloqueVentas(bloque, AvisoTestData.camposPorClave(bloque), ventas);
    }

    private static Registro gasto(String fecha, int total, String categoria) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(GASTO_FECHA_ID.toString(), fecha);
        valores.put(GASTO_TOTAL_ID.toString(), total);
        valores.put(GASTO_CATEGORIA_ID.toString(), categoria);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(GASTOS_ID)
                .valores(valores).build();
    }

    private static BloqueGastos bloqueDeGastos(List<Registro> gastos) {
        Campo fecha = campo(GASTO_FECHA_ID, GASTOS_ID, "date");
        Campo total = campo(GASTO_TOTAL_ID, GASTOS_ID, "total");
        Campo categoria = campo(GASTO_CATEGORIA_ID, GASTOS_ID, "category");
        Bloque bloque = Bloque.builder().id(GASTOS_ID).negocioId(AvisoTestData.NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.GASTOS).campos(List.of(fecha, total, categoria))
                .build();
        return new BloqueGastos(bloque, Map.of("date", fecha, "total", total, "category", categoria), gastos);
    }

    private static Campo campo(CampoId id, BloqueId bloqueId, String clave) {
        return Campo.builder().id(id).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(bloqueId).clave(clave).build();
    }
}
