package com.ensambla.backend.domain.mapper.inventario;

import com.ensambla.backend.domain.dto.inventario.ConsultarStockResponse;
import com.ensambla.backend.domain.dto.inventario.MovimientoItem;
import com.ensambla.backend.domain.dto.inventario.RegistrarMovimientoResponse;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.enums.TipoMovimiento;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventarioMappersTest {

    private static final UUID MOVIMIENTO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID HARINA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID AZUCAR_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID CAMPO_NOMBRE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");
    private static final LocalDate FECHA = LocalDate.parse("2026-10-12");

    private final MovimientoItemMapper movimientoItemMapper = new MovimientoItemMapper();

    @Test
    @DisplayName("Pasa el movimiento a su item con el nombre del producto y el tipo como texto")
    void toItemShouldMapMovimiento() {
        MovimientoItem result = movimientoItemMapper.toItem(givenMovimiento(HARINA_ID), "Harina");

        assertThat(result).isEqualTo(new MovimientoItem(
                MOVIMIENTO_ID, HARINA_ID, "Harina", "SALIDA", new BigDecimal("2"), FECHA, "Venta", AHORA));
    }

    @Test
    @DisplayName("Registrar devuelve el movimiento y el stock después de aplicarlo")
    void registrarShouldReturnMovimientoAndStock() {
        RegistrarMovimientoResponse result = new RegistrarMovimientoMapper(movimientoItemMapper)
                .toResponse(givenMovimiento(HARINA_ID), "Harina", new BigDecimal("8"));

        assertThat(result.movimiento().producto()).isEqualTo("Harina");
        assertThat(result.stockActual()).isEqualByComparingTo("8");
    }

    @Test
    @DisplayName("Listar pone a cada movimiento el nombre de su producto, o 'Sin nombre' si ya no está")
    void listarShouldNameEachMovimientoAfterItsProduct() {
        Registro harina = Registro.builder().id(HARINA_ID).valores(Map.of(CAMPO_NOMBRE_ID.toString(), "Harina")).build();

        List<MovimientoItem> result = new ListarMovimientosMapper(movimientoItemMapper).toResponse(
                List.of(givenMovimiento(HARINA_ID), givenMovimiento(AZUCAR_ID)), List.of(harina), givenCampos());

        assertThat(result).extracting(MovimientoItem::producto).containsExactly("Harina", "Sin nombre");
    }

    @Test
    @DisplayName("Stock arma cada fila con el nombre del producto y su stock, y conserva la paginación")
    void stockShouldPairEachProductWithItsStock() {
        Registro harina = Registro.builder().id(HARINA_ID).valores(Map.of(CAMPO_NOMBRE_ID.toString(), "Harina")).build();
        Pagina<Registro> pagina = new Pagina<>(List.of(harina), 1, 20, 41);

        ConsultarStockResponse result = new ConsultarStockMapper()
                .toResponse(pagina, Map.of(HARINA_ID, new BigDecimal("3")), givenCampos());

        assertThat(result.contenido()).singleElement().satisfies(item -> {
            assertThat(item.registroId()).isEqualTo(HARINA_ID);
            assertThat(item.producto()).isEqualTo("Harina");
            assertThat(item.stock()).isEqualByComparingTo("3");
        });
        assertThat(result.pagina()).isEqualTo(1);
        assertThat(result.tamanio()).isEqualTo(20);
        assertThat(result.totalElementos()).isEqualTo(41);
        assertThat(result.totalPaginas()).isEqualTo(3);
    }

    private static List<Campo> givenCampos() {
        return List.of(Campo.builder().id(CAMPO_NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build());
    }

    private static MovimientoStock givenMovimiento(UUID registroId) {
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
