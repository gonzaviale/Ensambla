package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AccionesDePrueba;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaRevisarPrecioTrasAumentoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID YERBA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final String NOMBRE = "Yerba Nobleza Gaucha 1 kg";
    private static final String AUMENTO = "2026-10-05T15:00:00Z";

    private final ReglaRevisarPrecioTrasAumento regla = new ReglaRevisarPrecioTrasAumento(
            new MetricasVentas(30, new BigDecimal("0.5")), 7, new BigDecimal("0.2"));

    private List<CambioPrecio> cambios = List.of(aumento(1000, 1200, AUMENTO));
    private List<Registro> ventas = ventasQueCaen();

    private TipoAviso tipo;
    private List<ResultadoRegla> resultados;
    private List<ResultadoRegla> resultadosDelEscenarioBase;

    @Test
    @DisplayName("Es la regla de los avisos de revisar el precio")
    void esLaReglaDeLosAvisosDeRevisarElPrecio() {
        cuandoSePideElTipo();

        entoncesElTipoEsRevisarPrecio();
    }

    @Test
    @DisplayName("Avisa cuando las ventas por día caen después de un aumento de precio")
    void avisaCuandoLasVentasPorDiaCaenDespuesDeUnAumentoDePrecio() {
        cuandoSeEvaluaElNegocio();

        entoncesAvisaQueLaYerbaVendeMenosDesdeQueSubioElPrecio();
        entoncesExplicaLaCaidaYElAumento();
    }

    @Test
    @DisplayName("Ofrece volver al precio anterior con una acción que edita el registro y otra que abre el producto")
    void ofreceVolverAlPrecioAnteriorConUnaAccionQueEditaElRegistroYOtraQueAbreElProducto() {
        cuandoSeEvaluaElNegocio();

        entoncesOfreceVolverAlPrecioAnteriorYAbrirElProducto();
    }

    @Test
    @DisplayName("No avisa si la caída de ventas es menor a la mínima")
    void noAvisaSiLaCaidaDeVentasEsMenorALaMinima() {
        dadoUnaCaidaDeVentasChica();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si las ventas se mantienen o suben")
    void noAvisaSiLasVentasSeMantienenOSuben() {
        dadoVentasQueSuben();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si no vendía antes del aumento: no hay con qué comparar")
    void noAvisaSiNoVendiaAntesDelAumentoNoHayConQueComparar() {
        dadoQueNoSeVendiaAntesDelAumento();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa mientras la ventana posterior al aumento no terminó")
    void noAvisaMientrasLaVentanaPosteriorAlAumentoNoTermino() {
        dadoUnAumentoReciente();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de un aumento más viejo que el período")
    void noAvisaDeUnAumentoMasViejoQueElPeriodo() {
        dadoUnAumentoMasViejoQueElPeriodo();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa si el último cambio fue una baja de precio: el aviso vence solo")
    void noAvisaSiElUltimoCambioFueUnaBajaDePrecioElAvisoVenceSolo() {
        dadoUnaBajaDePrecioDespuesDelAumento();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("Toma el último cambio de cada producto aunque los cambios lleguen desordenados")
    void tomaElUltimoCambioDeCadaProductoAunqueLosCambiosLleguenDesordenados() {
        dadoCambiosDesordenadosConUnAumentoDespuesDeOtro();

        cuandoSeEvaluaElNegocio();

        entoncesElAvisoEsDelAumentoDelDoceDeOctubre();
    }

    @Test
    @DisplayName("El primer precio que se carga no es un aumento")
    void elPrimerPrecioQueSeCargaNoEsUnAumento() {
        dadoElPrimerPrecioCargado();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa sin cambios de precio")
    void noAvisaSinCambiosDePrecio() {
        dadoQueNoHuboCambiosDePrecio();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa sin la fecha de hoy")
    void noAvisaSinLaFechaDeHoy() {
        cuandoSeEvaluaElNegocioSinLaFechaDeHoy();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("No avisa de un cambio de precio de un producto que ya no está en el inventario")
    void noAvisaDeUnCambioDePrecioDeUnProductoQueYaNoEstaEnElInventario() {
        dadoUnCambioDePrecioDeOtroProducto();

        cuandoSeEvaluaElNegocio();

        entoncesNoAvisaNada();
    }

    @Test
    @DisplayName("La huella no cambia con las ventas")
    void laHuellaNoCambiaConLasVentas() {
        dadoOtrasVentasConLaMismaCaida();

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase();
    }

    @Test
    @DisplayName("La huella cambia con un aumento nuevo")
    void laHuellaCambiaConUnAumentoNuevo() {
        dadoUnAumentoDeMilAMil300();

        cuandoSeEvaluaElNegocioYElEscenarioBase();

        entoncesLaHuellaDifiereDeLaDelEscenarioBase();
    }

    // --- dado ---

    private void dadoUnaCaidaDeVentasChica() {
        ventas = List.of(venta(14, "2026-10-01"), venta(12, "2026-10-06"));
    }

    private void dadoVentasQueSuben() {
        ventas = List.of(venta(14, "2026-10-01"), venta(20, "2026-10-06"));
    }

    private void dadoQueNoSeVendiaAntesDelAumento() {
        ventas = List.of(venta(7, "2026-10-06"));
    }

    private void dadoUnAumentoReciente() {
        cambios = List.of(aumento(1000, 1200, "2026-10-15T15:00:00Z"));
    }

    private void dadoUnAumentoMasViejoQueElPeriodo() {
        cambios = List.of(aumento(1000, 1200, "2026-09-10T15:00:00Z"));
        ventas = List.of(venta(14, "2026-09-05"), venta(7, "2026-09-12"));
    }

    private void dadoUnaBajaDePrecioDespuesDelAumento() {
        cambios = List.of(aumento(1000, 1200, AUMENTO), aumento(1200, 1000, "2026-10-12T15:00:00Z"));
    }

    private void dadoCambiosDesordenadosConUnAumentoDespuesDeOtro() {
        cambios = List.of(aumento(1000, 1200, "2026-10-12T15:00:00Z"), aumento(900, 1000, "2026-09-25T15:00:00Z"));
        ventas = List.of(venta(14, "2026-10-09"), venta(3, "2026-10-13"));
    }

    private void dadoElPrimerPrecioCargado() {
        cambios = List.of(new CambioPrecio(AvisoTestData.NEGOCIO_ID, RegistroId.de(YERBA_ID), null,
                new BigDecimal("1200"), OffsetDateTime.parse(AUMENTO)));
    }

    private void dadoQueNoHuboCambiosDePrecio() {
        cambios = List.of();
    }

    private void dadoUnCambioDePrecioDeOtroProducto() {
        cambios = List.of(new CambioPrecio(AvisoTestData.NEGOCIO_ID, RegistroId.nuevo(), new BigDecimal("1000"),
                new BigDecimal("1200"), OffsetDateTime.parse(AUMENTO)));
    }

    private void dadoOtrasVentasConLaMismaCaida() {
        ventas = List.of(venta(21, "2026-10-01"), venta(7, "2026-10-06"));
    }

    private void dadoUnAumentoDeMilAMil300() {
        cambios = List.of(aumento(1000, 1300, AUMENTO));
    }

    // --- cuando ---

    private void cuandoSePideElTipo() {
        tipo = regla.tipo();
    }

    private void cuandoSeEvaluaElNegocio() {
        resultados = evaluar(cambios, ventas);
    }

    private void cuandoSeEvaluaElNegocioSinLaFechaDeHoy() {
        ContextoNegocio sinHoy = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, null,
                List.of(AvisoTestData.inventarioConPrecios(yerba())), List.of(), AnalisisZona.sinZona(), cambios);
        resultados = regla.evaluar(sinHoy);
    }

    private void cuandoSeEvaluaElNegocioYElEscenarioBase() {
        cuandoSeEvaluaElNegocio();
        resultadosDelEscenarioBase = evaluar(List.of(aumento(1000, 1200, AUMENTO)), ventasQueCaen());
    }

    // --- entonces ---

    private void entoncesElTipoEsRevisarPrecio() {
        assertThat(tipo).isEqualTo(TipoAviso.REVISAR_PRECIO);
    }

    private void entoncesAvisaQueLaYerbaVendeMenosDesdeQueSubioElPrecio() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("registro:" + YERBA_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 2);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.titulo()).isEqualTo("Yerba Nobleza Gaucha 1 kg vende menos desde que subiste el precio");
        });
    }

    private void entoncesExplicaLaCaidaYElAumento() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.detalle()).isEqualTo("Lo subiste de $1.000 a $1.200 el 5/10 y desde entonces vendés "
                    + "1 por día, contra 2 antes. Revisá si el nuevo precio te conviene.");
            assertThat(resultado.motivos()).containsExactly(
                    "Vendías 2 unidades por día y ahora 1 (50 % menos).",
                    "El precio pasó de $1.000 a $1.200 (20 % más) el 5/10.");
            assertThat(resultado.datos()).containsEntry("precioAnterior", new BigDecimal("1000"))
                    .containsEntry("precioNuevo", new BigDecimal("1200")).containsEntry("fecha", "2026-10-05");
        });
    }

    private void entoncesOfreceVolverAlPrecioAnteriorYAbrirElProducto() {
        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).hasSize(2);
            assertThat(resultado.acciones().get(0))
                    .isEqualTo(AccionesDePrueba.cambiarPrecio("Volver a $1.000", YERBA_ID, "1000"));
            assertThat(resultado.acciones().get(1)).isEqualTo(AccionesDePrueba.abrirProducto(YERBA_ID));
        });
    }

    private void entoncesNoAvisaNada() {
        assertThat(resultados).isEmpty();
    }

    private void entoncesElAvisoEsDelAumentoDelDoceDeOctubre() {
        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.datos()).containsEntry("fecha", "2026-10-12"));
    }

    private void entoncesLaHuellaEsLaMismaQueLaDelEscenarioBase() {
        assertThat(huellaDe(resultados)).isEqualTo(huellaDe(resultadosDelEscenarioBase));
    }

    private void entoncesLaHuellaDifiereDeLaDelEscenarioBase() {
        assertThat(huellaDe(resultados)).isNotEqualTo(huellaDe(resultadosDelEscenarioBase));
    }

    // --- datos ---

    private List<ResultadoRegla> evaluar(List<CambioPrecio> cambiosDePrecio, List<Registro> ventasDelNegocio) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(yerba());
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario),
                List.of(AvisoTestData.ventas(ventasDelNegocio.toArray(new Registro[0]))), AnalisisZona.sinZona(),
                cambiosDePrecio));
    }

    private static String huellaDe(List<ResultadoRegla> resultados) {
        return resultados.get(0).huellaPara(TipoAviso.REVISAR_PRECIO);
    }

    private static Registro yerba() {
        return AvisoTestData.producto(YERBA_ID, NOMBRE, 40, 1200, 800);
    }

    /** 14 unidades en la semana anterior al aumento (2 por día) y 7 en la posterior (1 por día). */
    private static List<Registro> ventasQueCaen() {
        return List.of(venta(14, "2026-10-01"), venta(7, "2026-10-06"));
    }

    private static Registro venta(int cantidad, String fecha) {
        return AvisoTestData.venta(YERBA_ID.toString(), cantidad, 1000, fecha, null);
    }

    private static CambioPrecio aumento(int anterior, int nuevo, String instante) {
        return new CambioPrecio(AvisoTestData.NEGOCIO_ID, RegistroId.de(YERBA_ID), new BigDecimal(anterior),
                new BigDecimal(nuevo), OffsetDateTime.parse(instante));
    }
}
