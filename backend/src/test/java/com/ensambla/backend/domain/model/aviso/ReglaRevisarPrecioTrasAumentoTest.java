package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaRevisarPrecioTrasAumentoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final UUID YERBA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final String NOMBRE = "Yerba Nobleza Gaucha 1 kg";
    private static final String AUMENTO = "2026-10-05T15:00:00Z";

    private final ReglaRevisarPrecioTrasAumento regla = new ReglaRevisarPrecioTrasAumento(
            new MetricasVentas(30, new BigDecimal("0.5")), 7, new BigDecimal("0.2"));

    @Test
    @DisplayName("Es la regla de los avisos de revisar el precio")
    void tipoShouldBeRevisarPrecio() {
        assertThat(regla.tipo()).isEqualTo(TipoAviso.REVISAR_PRECIO);
    }

    @Test
    @DisplayName("Avisa cuando las ventas por día caen después de un aumento de precio")
    void evaluarShouldWarnWhenSalesDropAfterPriceIncrease() {
        List<ResultadoRegla> resultados = whenEvaluar(List.of(aumento(1000, 1200, AUMENTO)), ventasQueCaen());

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.entidad()).isEqualTo("registro:" + YERBA_ID);
            assertThat(resultado.prioridad()).isEqualTo((short) 2);
            assertThat(resultado.proyectoId()).isEqualTo(AvisoTestData.PROYECTO_ID);
            assertThat(resultado.titulo()).isEqualTo("Yerba Nobleza Gaucha 1 kg vende menos desde que subiste el precio");
            assertThat(resultado.detalle()).isEqualTo("Lo subiste de $1.000 a $1.200 el 5/10 y desde entonces vendés "
                    + "1 por día, contra 2 antes. Revisá si el nuevo precio te conviene.");
            assertThat(resultado.motivos()).containsExactly(
                    "Vendías 2 unidades por día y ahora 1 (50 % menos).",
                    "El precio pasó de $1.000 a $1.200 (20 % más) el 5/10.");
            assertThat(resultado.datos()).containsEntry("precioAnterior", new BigDecimal("1000"))
                    .containsEntry("precioNuevo", new BigDecimal("1200")).containsEntry("fecha", "2026-10-05");
        });
    }

    @Test
    @DisplayName("Ofrece volver al precio anterior con una acción que edita el registro y otra que abre el producto")
    void evaluarShouldOfferVolverAndAbrirActions() {
        List<ResultadoRegla> resultados = whenEvaluar(List.of(aumento(1000, 1200, AUMENTO)), ventasQueCaen());

        assertThat(resultados).singleElement().satisfies(resultado -> {
            assertThat(resultado.acciones()).hasSize(2);
            assertThat(resultado.acciones().get(0)).isEqualTo(Map.of(
                    "tipo", "EDITAR_REGISTRO",
                    "texto", "Volver a $1.000",
                    "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                    "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                    "registroId", YERBA_ID.toString(),
                    "valores", Map.of("price", new BigDecimal("1000"))));
            assertThat(resultado.acciones().get(1)).isEqualTo(Map.of(
                    "tipo", "ABRIR",
                    "texto", "Ver producto",
                    "proyectoId", AvisoTestData.PROYECTO_ID.toString(),
                    "bloqueId", AvisoTestData.INVENTARIO_ID.toString(),
                    "registroId", YERBA_ID.toString()));
        });
    }

    @Test
    @DisplayName("No avisa si la caída de ventas es menor a la mínima")
    void evaluarShouldReturnNothingWhenDropIsSmall() {
        List<Registro> ventas = List.of(venta(14, "2026-10-01"), venta(12, "2026-10-06"));

        assertThat(whenEvaluar(List.of(aumento(1000, 1200, AUMENTO)), ventas)).isEmpty();
    }

    @Test
    @DisplayName("No avisa si las ventas se mantienen o suben")
    void evaluarShouldReturnNothingWhenSalesDoNotDrop() {
        List<Registro> ventas = List.of(venta(14, "2026-10-01"), venta(20, "2026-10-06"));

        assertThat(whenEvaluar(List.of(aumento(1000, 1200, AUMENTO)), ventas)).isEmpty();
    }

    @Test
    @DisplayName("No avisa si no vendía antes del aumento: no hay con qué comparar")
    void evaluarShouldReturnNothingWhenThereWereNoSalesBefore() {
        List<Registro> ventas = List.of(venta(7, "2026-10-06"));

        assertThat(whenEvaluar(List.of(aumento(1000, 1200, AUMENTO)), ventas)).isEmpty();
    }

    @Test
    @DisplayName("No avisa mientras la ventana posterior al aumento no terminó")
    void evaluarShouldReturnNothingWhenWindowAfterIncreaseIsStillOpen() {
        CambioPrecio reciente = aumento(1000, 1200, "2026-10-15T15:00:00Z");

        assertThat(whenEvaluar(List.of(reciente), ventasQueCaen())).isEmpty();
    }

    @Test
    @DisplayName("No avisa de un aumento más viejo que el período")
    void evaluarShouldReturnNothingWhenIncreaseIsOlderThanThePeriod() {
        CambioPrecio viejo = aumento(1000, 1200, "2026-09-10T15:00:00Z");
        List<Registro> ventas = List.of(venta(14, "2026-09-05"), venta(7, "2026-09-12"));

        assertThat(whenEvaluar(List.of(viejo), ventas)).isEmpty();
    }

    @Test
    @DisplayName("No avisa si el último cambio fue una baja de precio: el aviso vence solo")
    void evaluarShouldReturnNothingWhenLastChangeWasADecrease() {
        List<CambioPrecio> cambios = List.of(
                aumento(1000, 1200, AUMENTO), aumento(1200, 1000, "2026-10-12T15:00:00Z"));

        assertThat(whenEvaluar(cambios, ventasQueCaen())).isEmpty();
    }

    @Test
    @DisplayName("Toma el último cambio de cada producto aunque los cambios lleguen desordenados")
    void evaluarShouldUseTheLatestChangeEvenWhenUnsorted() {
        List<CambioPrecio> cambios = List.of(
                aumento(1000, 1200, "2026-10-12T15:00:00Z"), aumento(900, 1000, "2026-09-25T15:00:00Z"));
        List<Registro> ventas = List.of(venta(14, "2026-10-09"), venta(3, "2026-10-13"));

        List<ResultadoRegla> resultados = whenEvaluar(cambios, ventas);

        assertThat(resultados).singleElement().satisfies(resultado ->
                assertThat(resultado.datos()).containsEntry("fecha", "2026-10-12"));
    }

    @Test
    @DisplayName("El primer precio que se carga no es un aumento")
    void evaluarShouldReturnNothingForTheFirstPrice() {
        CambioPrecio primero = new CambioPrecio(AvisoTestData.NEGOCIO_ID, RegistroId.de(YERBA_ID), null,
                new BigDecimal("1200"), OffsetDateTime.parse(AUMENTO));

        assertThat(whenEvaluar(List.of(primero), ventasQueCaen())).isEmpty();
    }

    @Test
    @DisplayName("No avisa sin cambios de precio ni sin la fecha de hoy")
    void evaluarShouldReturnNothingWithoutChangesOrHoy() {
        assertThat(whenEvaluar(List.of(), ventasQueCaen())).isEmpty();

        ContextoNegocio sinHoy = new ContextoNegocio(AvisoTestData.NEGOCIO_ID, null,
                List.of(AvisoTestData.inventarioConPrecios(yerba())), List.of(), AnalisisZona.sinZona(),
                List.of(aumento(1000, 1200, AUMENTO)));
        assertThat(regla.evaluar(sinHoy)).isEmpty();
    }

    @Test
    @DisplayName("No avisa de un cambio de precio de un producto que ya no está en el inventario")
    void evaluarShouldReturnNothingForUnknownProduct() {
        CambioPrecio deOtro = new CambioPrecio(AvisoTestData.NEGOCIO_ID, RegistroId.nuevo(), new BigDecimal("1000"),
                new BigDecimal("1200"), OffsetDateTime.parse(AUMENTO));

        assertThat(whenEvaluar(List.of(deOtro), ventasQueCaen())).isEmpty();
    }

    @Test
    @DisplayName("La huella no cambia con las ventas pero sí con un aumento nuevo")
    void evaluarShouldKeepHuellaWhenOnlySalesChangeAndChangeItWithANewIncrease() {
        ResultadoRegla base = whenEvaluar(List.of(aumento(1000, 1200, AUMENTO)), ventasQueCaen()).get(0);
        List<Registro> otrasVentas = List.of(venta(21, "2026-10-01"), venta(7, "2026-10-06"));
        ResultadoRegla conOtrasVentas = whenEvaluar(List.of(aumento(1000, 1200, AUMENTO)), otrasVentas).get(0);
        ResultadoRegla otroAumento = whenEvaluar(List.of(aumento(1000, 1300, AUMENTO)), ventasQueCaen()).get(0);

        assertThat(conOtrasVentas.huellaPara(TipoAviso.REVISAR_PRECIO))
                .isEqualTo(base.huellaPara(TipoAviso.REVISAR_PRECIO));
        assertThat(otroAumento.huellaPara(TipoAviso.REVISAR_PRECIO))
                .isNotEqualTo(base.huellaPara(TipoAviso.REVISAR_PRECIO));
    }

    // --- act ---
    private List<ResultadoRegla> whenEvaluar(List<CambioPrecio> cambios, List<Registro> ventas) {
        BloqueInventario inventario = AvisoTestData.inventarioConPrecios(yerba());
        return regla.evaluar(new ContextoNegocio(AvisoTestData.NEGOCIO_ID, HOY, List.of(inventario),
                List.of(AvisoTestData.ventas(ventas.toArray(new Registro[0]))), AnalisisZona.sinZona(), cambios));
    }

    // --- helpers ---
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
