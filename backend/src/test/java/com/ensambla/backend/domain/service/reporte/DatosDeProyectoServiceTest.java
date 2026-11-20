package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatosDeProyectoServiceTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId ENTRADAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000003");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final BloqueId NOTA_ID = BloqueId.de("b0000000-0000-4000-8000-000000000005");
    private static final CampoId TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");

    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final VentaCajaRepository ventaCajaRepository = mock(VentaCajaRepository.class);
    private final CobroCajaRepository cobroCajaRepository = mock(CobroCajaRepository.class);
    private final DatosDeProyectoService service = new DatosDeProyectoService(
            registroRepository, ventaCajaRepository, cobroCajaRepository);

    private Proyecto proyecto;
    private Sistema sistema;
    private Registro filaReal;
    private Registro venta;
    private Registro gasto;
    private Registro clienteRosa;
    private DatosDeProyecto datos;

    @Test
    @DisplayName("Carga las tablas de inventario, ventas, gastos y entradas con sus campos por clave y sin las filas de ejemplo")
    void cargaLasTablasDeInventarioVentasGastosYEntradasConSusCamposPorClaveYSinLasFilasDeEjemplo() {
        dadoUnProyectoConInventarioEntradasVentasGastosYUnaNota();

        cuandoSeCargaElProyecto();

        entoncesSeCargaElInventarioSinLaFilaDeEjemplo();
        entoncesSeCarganLasVentasYLasEntradas();
        entoncesSeCarganLosGastosConSuImporte();
    }

    @Test
    @DisplayName("Sin tabla de entradas el inventario no la nombra, y una tabla de gastos sin importe no se puede sumar")
    void sinTablaDeEntradasElInventarioNoLaNombraYUnaTablaDeGastosSinImporteNoSePuedeSumar() {
        dadoUnProyectoSinEntradasNiImporteDeGastos();

        cuandoSeCargaElProyecto();

        entoncesElInventarioNoNombraEntradasYLosGastosNoTienenImporte();
    }

    @Test
    @DisplayName("Carga el nombre de cada fila de las tablas de clientes, sin las de ejemplo ni las sin nombre")
    void cargaElNombreDeCadaFilaDeLasTablasDeClientesSinLasDeEjemploNiLasSinNombre() {
        dadoDosTablasDeClientesConFilasDeEjemploEnBlancoYSinNombre();

        cuandoSeCargaElProyecto();

        entoncesSoloSeCargaElNombreDeDonaRosa();
    }

    @Test
    @DisplayName("Reutiliza las filas de un sistema cargado y excluye sus ejemplos sin consultar el repositorio")
    void reutilizaLasFilasDeUnSistemaCargadoYExcluyeSusEjemplosSinConsultarElRepositorio() {
        dadoUnSistemaCargadoConUnaFilaRealYUnaDeEjemplo();

        cuandoSeCargaElSistema();

        entoncesSoloQuedaLaFilaRealSinConsultarElRepositorio();
    }

    @Test
    @DisplayName("Carga ventas y cobros de todas las cajas del proyecto dentro del negocio")
    void cargaVentasYCobrosDeTodasLasCajasDelProyectoDentroDelNegocio() {
        dadoUnSistemaConVentasYCobrosDeCaja();

        cuandoSeCargaElSistema();

        entoncesSeCarganLasVentasYLosCobrosDeCajaSinConsultarRegistros();
    }

    // --- dado ---

    private void dadoUnProyectoConInventarioEntradasVentasGastosYUnaNota() {
        filaReal = unRegistro(INVENTARIO_ID, false);
        venta = unRegistro(VENTAS_ID, false);
        gasto = unRegistro(GASTOS_ID, false);
        dadoLasFilasDelBloque(INVENTARIO_ID, filaReal, unRegistro(INVENTARIO_ID, true));
        dadoLasFilasDelBloque(VENTAS_ID, venta);
        dadoLasFilasDelBloque(GASTOS_ID, gasto);
        proyecto = Proyecto.builder().nombre("Almacén").bloques(List.of(
                unBloque(INVENTARIO_ID, TipoBloque.INVENTARIO, campoDe("name"), campoDe(null)),
                unBloque(ENTRADAS_ID, TipoBloque.ENTRADAS),
                unBloque(VENTAS_ID, TipoBloque.VENTAS, campoDe("product")),
                unBloque(GASTOS_ID, TipoBloque.GASTOS, campoDelTotal()),
                unBloque(NOTA_ID, TipoBloque.NOTA))).build();
    }

    private void dadoUnProyectoSinEntradasNiImporteDeGastos() {
        dadoLasFilasDelBloque(INVENTARIO_ID);
        dadoLasFilasDelBloque(GASTOS_ID);
        proyecto = Proyecto.builder().nombre("Almacén").bloques(List.of(
                unBloque(INVENTARIO_ID, TipoBloque.INVENTARIO), unBloque(GASTOS_ID, TipoBloque.GASTOS))).build();
    }

    private void dadoDosTablasDeClientesConFilasDeEjemploEnBlancoYSinNombre() {
        CampoId nombreId = CampoId.de("f0000000-0000-4000-8000-000000000031");
        BloqueId clientesId = BloqueId.de("b0000000-0000-4000-8000-000000000006");
        BloqueId clientesSinNombreId = BloqueId.de("b0000000-0000-4000-8000-000000000007");
        clienteRosa = unClienteConNombre(clientesId, nombreId, "  Doña Rosa ", false);
        dadoLasFilasDelBloque(clientesId, clienteRosa,
                unClienteConNombre(clientesId, nombreId, "Cliente de ejemplo", true),
                unClienteConNombre(clientesId, nombreId, "   ", false),
                unClienteSinNombre(clientesId));
        dadoLasFilasDelBloque(clientesSinNombreId);
        proyecto = Proyecto.builder().nombre("Almacén").bloques(List.of(
                Bloque.builder().id(clientesId).negocioId(NEGOCIO_ID).tipo(TipoBloque.CLIENTES)
                        .campos(List.of(Campo.builder().id(nombreId).clave("name").build())).build(),
                unBloque(clientesSinNombreId, TipoBloque.CLIENTES, campoDe("phone")))).build();
    }

    private void dadoUnSistemaCargadoConUnaFilaRealYUnaDeEjemplo() {
        filaReal = unRegistro(INVENTARIO_ID, false);
        Bloque inventario = unBloque(INVENTARIO_ID, TipoBloque.INVENTARIO, campoDe("name"));
        Proyecto almacen = Proyecto.builder().nombre("Almacén").bloques(List.of(inventario)).build();
        sistema = new Sistema(almacen, List.of(),
                Map.of(INVENTARIO_ID, List.of(filaReal, unRegistro(INVENTARIO_ID, true))));
    }

    private void dadoUnSistemaConVentasYCobrosDeCaja() {
        sistema = CajaTestData.sistema();
        when(ventaCajaRepository.listarPorProyecto(CajaTestData.NEGOCIO, CajaTestData.PROYECTO))
                .thenReturn(List.of(CobroCajaTestData.venta()));
        when(cobroCajaRepository.listarPorProyecto(CajaTestData.NEGOCIO, CajaTestData.PROYECTO))
                .thenReturn(List.of(CobroCajaTestData.cobro()));
    }

    private void dadoLasFilasDelBloque(BloqueId bloqueId, Registro... filas) {
        when(registroRepository.findAllByBloqueIdAndNegocioId(bloqueId, NEGOCIO_ID)).thenReturn(List.of(filas));
    }

    // --- cuando ---

    private void cuandoSeCargaElProyecto() {
        datos = service.cargar(proyecto, NEGOCIO_ID);
    }

    private void cuandoSeCargaElSistema() {
        datos = service.cargar(sistema);
    }

    // --- entonces ---

    private void entoncesSeCargaElInventarioSinLaFilaDeEjemplo() {
        assertThat(datos.nombre()).isEqualTo("Almacén");
        assertThat(datos.inventarios()).singleElement().satisfies(inventario -> {
            assertThat(inventario.bloque().getId()).isEqualTo(INVENTARIO_ID);
            assertThat(inventario.entradasId()).isEqualTo(ENTRADAS_ID);
            assertThat(inventario.camposPorClave()).containsOnlyKeys("name");
            assertThat(inventario.registros()).containsExactly(filaReal);
        });
    }

    private void entoncesSeCarganLasVentasYLasEntradas() {
        assertThat(datos.ventas()).singleElement().satisfies(ventas -> {
            assertThat(ventas.bloque().getId()).isEqualTo(VENTAS_ID);
            assertThat(ventas.registros()).containsExactly(venta);
        });
        assertThat(datos.entradas()).singleElement().satisfies(entradas -> {
            assertThat(entradas.bloque().getId()).isEqualTo(ENTRADAS_ID);
            assertThat(entradas.registros()).isEmpty();
        });
    }

    private void entoncesSeCarganLosGastosConSuImporte() {
        assertThat(datos.gastos()).singleElement().satisfies(gastos -> {
            assertThat(gastos.bloque().getId()).isEqualTo(GASTOS_ID);
            assertThat(gastos.tieneImporte()).isTrue();
            assertThat(gastos.registros()).containsExactly(gasto);
            assertThat(gastos.valorDe(gasto, "total")).isEqualTo(1500);
            assertThat(gastos.valorDe(gasto, "date")).isNull();
        });
    }

    private void entoncesElInventarioNoNombraEntradasYLosGastosNoTienenImporte() {
        assertThat(datos.inventarios().getFirst().entradasId()).isNull();
        assertThat(datos.ventas()).isEmpty();
        assertThat(datos.gastos().getFirst().tieneImporte()).isFalse();
    }

    private void entoncesSoloSeCargaElNombreDeDonaRosa() {
        assertThat(datos.nombresDeClientes()).containsOnly(Map.entry(clienteRosa.getId().toString(), "Doña Rosa"));
    }

    private void entoncesSoloQuedaLaFilaRealSinConsultarElRepositorio() {
        assertThat(datos.inventarios()).singleElement().satisfies(resultado ->
                assertThat(resultado.registros()).containsExactly(filaReal));
        verifyNoInteractions(registroRepository);
    }

    private void entoncesSeCarganLasVentasYLosCobrosDeCajaSinConsultarRegistros() {
        assertThat(datos.ventasCaja()).containsExactly(CobroCajaTestData.venta());
        assertThat(datos.cobrosCaja()).containsExactly(CobroCajaTestData.cobro());
        verifyNoInteractions(registroRepository);
    }

    // --- datos ---

    private static Registro unClienteConNombre(BloqueId bloqueId, CampoId nombreId, String nombre, boolean esEjemplo) {
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(bloqueId).esEjemplo(esEjemplo)
                .valores(Map.of(nombreId.toString(), nombre)).build();
    }

    private static Registro unClienteSinNombre(BloqueId bloqueId) {
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(bloqueId)
                .valores(Map.of()).build();
    }

    private static Bloque unBloque(BloqueId id, TipoBloque tipo, Campo... campos) {
        return Bloque.builder().id(id).negocioId(NEGOCIO_ID).tipo(tipo).campos(List.of(campos)).build();
    }

    private static Campo campoDe(String clave) {
        return Campo.builder().id(CampoId.nuevo()).clave(clave).build();
    }

    private static Campo campoDelTotal() {
        return Campo.builder().id(TOTAL_ID).clave("total").build();
    }

    private static Registro unRegistro(BloqueId bloqueId, boolean esEjemplo) {
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(bloqueId).esEjemplo(esEjemplo)
                .valores(Map.of(TOTAL_ID.toString(), 1500)).build();
    }
}
