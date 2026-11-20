package com.ensambla.backend.domain.useCase.inventario;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockResponse;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InventarioDtosTest {

    private static final MovimientoStockId MOVIMIENTO_ID = MovimientoStockId.de("e0000000-0000-4000-8000-000000000001");
    private static final RegistroId HARINA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId AZUCAR_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final CampoId CAMPO_NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");
    private static final LocalDate FECHA = LocalDate.parse("2026-10-12");

    private MovimientoItem item;
    private RegistrarMovimientoResponse respuestaDeRegistro;
    private List<MovimientoItem> items;
    private ConsultarStockResponse respuestaDeStock;

    @Test
    @DisplayName("Pasa el movimiento a su item con el nombre del producto y el tipo como texto")
    void pasaElMovimientoASuItemConElNombreDelProductoYElTipoComoTexto() {
        cuandoSeArmaElItemDelMovimientoDeHarina();

        entoncesElItemCopiaElMovimientoConElNombreDeHarina();
    }

    @Test
    @DisplayName("Registrar devuelve el movimiento y el stock después de aplicarlo")
    void registrarDevuelveElMovimientoYElStockDespuesDeAplicarlo() {
        cuandoSeArmaLaRespuestaDeRegistrarConStockDe("8");

        entoncesLaRespuestaTieneElProductoHarinaYStockDe("8");
    }

    @Test
    @DisplayName("Listar pone a cada movimiento el nombre de su producto, o 'Sin nombre' si ya no está")
    void listarPoneACadaMovimientoElNombreDeSuProductoOSinNombreSiYaNoEsta() {
        cuandoSeListanMovimientosDeHarinaYDeUnProductoQueYaNoEsta();

        entoncesLosProductosSonHarinaYSinNombre();
    }

    @Test
    @DisplayName("Stock arma cada fila con el nombre del producto y su stock, y conserva la paginación")
    void stockArmaCadaFilaConElNombreDelProductoYSuStockYConservaLaPaginacion() {
        cuandoSeArmaLaRespuestaDeStockDeUnaPaginaConHarinaYStockDe("3");

        entoncesLaFilaMuestraHarinaConStockDe("3");
        entoncesSeConservaLaPaginacion();
    }

    // --- cuando ---

    private void cuandoSeArmaElItemDelMovimientoDeHarina() {
        item = MovimientoItem.de(unMovimientoDe(HARINA_ID), "Harina");
    }

    private void cuandoSeArmaLaRespuestaDeRegistrarConStockDe(String stock) {
        respuestaDeRegistro = RegistrarMovimientoResponse.de(unMovimientoDe(HARINA_ID), "Harina", new BigDecimal(stock));
    }

    private void cuandoSeListanMovimientosDeHarinaYDeUnProductoQueYaNoEsta() {
        items = MovimientoItem.de(
                List.of(unMovimientoDe(HARINA_ID), unMovimientoDe(AZUCAR_ID)), List.of(unaHarina()), unosCampos());
    }

    private void cuandoSeArmaLaRespuestaDeStockDeUnaPaginaConHarinaYStockDe(String stock) {
        Pagina<Registro> pagina = new Pagina<>(List.of(unaHarina()), 1, 20, 41);
        respuestaDeStock = ConsultarStockResponse.de(pagina, Map.of(HARINA_ID, new BigDecimal(stock)), unosCampos());
    }

    // --- entonces ---

    private void entoncesElItemCopiaElMovimientoConElNombreDeHarina() {
        assertThat(item).isEqualTo(new MovimientoItem(
                MOVIMIENTO_ID, HARINA_ID, "Harina", "SALIDA", new BigDecimal("2"), FECHA, "Venta", AHORA));
    }

    private void entoncesLaRespuestaTieneElProductoHarinaYStockDe(String stock) {
        assertThat(respuestaDeRegistro.movimiento().producto()).isEqualTo("Harina");
        assertThat(respuestaDeRegistro.stockActual()).isEqualByComparingTo(stock);
    }

    private void entoncesLosProductosSonHarinaYSinNombre() {
        assertThat(items).extracting(MovimientoItem::producto).containsExactly("Harina", "Sin nombre");
    }

    private void entoncesLaFilaMuestraHarinaConStockDe(String stock) {
        assertThat(respuestaDeStock.contenido()).singleElement().satisfies(fila -> {
            assertThat(fila.registroId()).isEqualTo(HARINA_ID);
            assertThat(fila.producto()).isEqualTo("Harina");
            assertThat(fila.stock()).isEqualByComparingTo(stock);
        });
    }

    private void entoncesSeConservaLaPaginacion() {
        assertThat(respuestaDeStock.pagina()).isEqualTo(1);
        assertThat(respuestaDeStock.tamanio()).isEqualTo(20);
        assertThat(respuestaDeStock.totalElementos()).isEqualTo(41);
        assertThat(respuestaDeStock.totalPaginas()).isEqualTo(3);
    }

    // --- datos ---

    private static Registro unaHarina() {
        return Registro.builder().id(HARINA_ID).valores(Map.of(CAMPO_NOMBRE_ID.toString(), "Harina")).build();
    }

    private static List<Campo> unosCampos() {
        return List.of(Campo.builder().id(CAMPO_NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build());
    }

    private static MovimientoStock unMovimientoDe(RegistroId registroId) {
        return MovimientoStock.builder()
                .id(MOVIMIENTO_ID)
                .registroId(registroId)
                .tipo(TipoMovimiento.SALIDA)
                .cantidad(new BigDecimal("2"))
                .fecha(FECHA)
                .nota("Venta")
                .creadoEn(AHORA)
                .build();
    }
}
