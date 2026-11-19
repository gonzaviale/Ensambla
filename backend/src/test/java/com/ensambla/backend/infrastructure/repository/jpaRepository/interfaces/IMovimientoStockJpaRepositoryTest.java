package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.MovimientoStockEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IMovimientoStockJpaRepositoryTest extends PostgresDatosTestSupport {

    @Autowired
    private IMovimientoStockJpaRepository movimientos;

    private UUID negocioA;
    private UUID negocioB;
    private UUID bloqueA;
    private UUID harina;
    private UUID azucar;

    @BeforeEach
    void dadoUnBloqueConDosProductos() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueA = givenBloque(NegocioId.de(negocioA), "Productos");
        givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueA), "Producto", TipoCampo.TEXTO, 0);
        harina = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), AHORA);
        azucar = givenRegistro(NegocioId.de(negocioA), BloqueId.de(bloqueA), Map.of(), AHORA);
    }

    private List<IMovimientoStockJpaRepository.StockPorRegistro> stock;
    private List<MovimientoStockEntity> todos;
    private List<MovimientoStockEntity> deHarina;
    private List<MovimientoStockEntity> limitados;
    private MovimientoStockEntity viejo;
    private MovimientoStockEntity medio;
    private MovimientoStockEntity nuevo;
    private Throwable rechazo;

    @Test
    @DisplayName("El stock es la suma de entradas menos salidas de cada producto")
    void elStockEsLaSumaDeEntradasMenosSalidasDeCadaProducto() {
        dadoMovimientosDeHarinaYDeAzucar();

        cuandoSeConsultaElStockDeHarinaYAzucar();

        entoncesElStockDeHarinaEsOchoYMedioYElDeAzucarCuatro();
    }

    @Test
    @DisplayName("Un producto sin movimientos no aparece y un negocio ajeno no suma")
    void unProductoSinMovimientosNoApareceYUnNegocioAjenoNoSuma() {
        dadoUnaEntradaDeHarina();

        entoncesElAzucarSinMovimientosYElNegocioAjenoNoTienenStock();
    }

    @Test
    @DisplayName("Los movimientos recientes salen del bloque, los más nuevos primero, y se pueden acotar a un producto")
    void losMovimientosRecientesSalenDelBloqueLosMasNuevosPrimeroYSePuedenAcotarAUnProducto() {
        dadoTresMovimientosDelBloqueYUnoDeOtroBloque();

        cuandoSeListanLosMovimientosRecientesDelBloque();

        entoncesSalenLosMasNuevosPrimeroAcotadosPorProductoYPorLimite();
    }

    @Test
    @DisplayName("Un negocio ajeno no ve los movimientos del bloque")
    void unNegocioAjenoNoVeLosMovimientosDelBloque() {
        dadoUnaEntradaDeHarina();

        entoncesElNegocioAjenoNoVeLosMovimientosDelBloque();
    }

    @Test
    @DisplayName("No se guarda un movimiento de un producto de otro negocio")
    void noSeGuardaUnMovimientoDeUnProductoDeOtroNegocio() {
        cuandoSeIntentaGuardarUnMovimientoDeHarinaConElNegocioB();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("No se guarda una cantidad en cero")
    void noSeGuardaUnaCantidadEnCero() {
        cuandoSeIntentaGuardarUnMovimientoConCantidadCero();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("Al borrar el producto se borran sus movimientos")
    void alBorrarElProductoSeBorranSusMovimientos() {
        dadoUnaEntradaDeHarinaDeHaceCeroMinutos();

        cuandoSeBorraElProductoHarina();

        entoncesNoQuedanMovimientosEnElBloque();
    }

    // --- dado ---
    private void dadoMovimientosDeHarinaYDeAzucar() {
        unMovimientoGuardado(negocioA, harina, TipoMovimiento.ENTRADA, "10", 3);
        unMovimientoGuardado(negocioA, harina, TipoMovimiento.SALIDA, "2.5", 2);
        unMovimientoGuardado(negocioA, harina, TipoMovimiento.ENTRADA, "1", 1);
        unMovimientoGuardado(negocioA, azucar, TipoMovimiento.ENTRADA, "4", 1);
    }

    private void dadoUnaEntradaDeHarina() {
        unMovimientoGuardado(negocioA, harina, TipoMovimiento.ENTRADA, "10", 1);
    }

    private void dadoUnaEntradaDeHarinaDeHaceCeroMinutos() {
        unMovimientoGuardado(negocioA, harina, TipoMovimiento.ENTRADA, "1", 0);
    }

    private void dadoTresMovimientosDelBloqueYUnoDeOtroBloque() {
        viejo = unMovimientoGuardado(negocioA, harina, TipoMovimiento.ENTRADA, "1", 3);
        medio = unMovimientoGuardado(negocioA, azucar, TipoMovimiento.ENTRADA, "1", 2);
        nuevo = unMovimientoGuardado(negocioA, harina, TipoMovimiento.SALIDA, "1", 1);
        UUID otroBloque = givenBloque(NegocioId.de(negocioA), "Clientes");
        UUID otroProducto = givenRegistro(NegocioId.de(negocioA), BloqueId.de(otroBloque), Map.of(), AHORA);
        unMovimientoGuardado(negocioA, otroProducto, TipoMovimiento.ENTRADA, "1", 0);
    }

    private static BigDecimal stockEn(List<IMovimientoStockJpaRepository.StockPorRegistro> filas, UUID registroId) {
        return filas.stream()
                .filter(fila -> fila.getRegistro().equals(registroId))
                .findFirst()
                .orElseThrow()
                .getStock();
    }

    private MovimientoStockEntity unMovimientoGuardado(UUID negocioId, UUID registroId, TipoMovimiento tipo,
                                                       String cantidad, int minutosAtras) {
        return movimientos.saveAndFlush(unMovimiento(negocioId, registroId, tipo, cantidad, minutosAtras));
    }

    private static MovimientoStockEntity unMovimiento(UUID negocioId, UUID registroId, TipoMovimiento tipo,
                                                      String cantidad, int minutosAtras) {
        return MovimientoStockEntity.builder()
                .negocioId(negocioId)
                .registroId(registroId)
                .tipo(tipo)
                .cantidad(new BigDecimal(cantidad))
                .fecha(LocalDate.parse("2026-10-12"))
                .creadoEn(OffsetDateTime.parse("2026-10-12T12:00:00Z").minusMinutes(minutosAtras))
                .build();
    }

    // --- cuando ---
    private void cuandoSeConsultaElStockDeHarinaYAzucar() {
        stock = movimientos.stockDe(negocioA, List.of(harina, azucar));
    }

    private void cuandoSeListanLosMovimientosRecientesDelBloque() {
        todos = movimientos.findRecientes(negocioA, bloqueA, null, 100);
        deHarina = movimientos.findRecientes(negocioA, bloqueA, harina.toString(), 100);
        limitados = movimientos.findRecientes(negocioA, bloqueA, null, 1);
    }

    private void cuandoSeIntentaGuardarUnMovimientoDeHarinaConElNegocioB() {
        MovimientoStockEntity ajeno = unMovimiento(negocioB, harina, TipoMovimiento.ENTRADA, "1", 0);
        rechazo = catchThrowable(() -> movimientos.saveAndFlush(ajeno));
    }

    private void cuandoSeIntentaGuardarUnMovimientoConCantidadCero() {
        MovimientoStockEntity vacio = unMovimiento(negocioA, harina, TipoMovimiento.ENTRADA, "0", 0);
        rechazo = catchThrowable(() -> movimientos.saveAndFlush(vacio));
    }

    private void cuandoSeBorraElProductoHarina() {
        registros.deleteById(harina);
        registros.flush();
        movimientos.flush();
    }

    // --- entonces ---
    private void entoncesElStockDeHarinaEsOchoYMedioYElDeAzucarCuatro() {
        assertThat(stock).hasSize(2);
        assertThat(stockEn(stock, harina)).isEqualByComparingTo("8.5");
        assertThat(stockEn(stock, azucar)).isEqualByComparingTo("4");
    }

    private void entoncesElAzucarSinMovimientosYElNegocioAjenoNoTienenStock() {
        assertThat(movimientos.stockDe(negocioA, List.of(azucar))).isEmpty();
        assertThat(movimientos.stockDe(negocioB, List.of(harina))).isEmpty();
    }

    private void entoncesSalenLosMasNuevosPrimeroAcotadosPorProductoYPorLimite() {
        assertThat(todos).extracting(MovimientoStockEntity::getId)
                .containsExactly(nuevo.getId(), medio.getId(), viejo.getId());
        assertThat(deHarina).extracting(MovimientoStockEntity::getId).containsExactly(nuevo.getId(), viejo.getId());
        assertThat(limitados).extracting(MovimientoStockEntity::getId).containsExactly(nuevo.getId());
    }

    private void entoncesElNegocioAjenoNoVeLosMovimientosDelBloque() {
        assertThat(movimientos.findRecientes(negocioB, bloqueA, null, 100)).isEmpty();
    }

    private void entoncesLaBaseRechazaPorIntegridad() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesNoQuedanMovimientosEnElBloque() {
        assertThat(movimientos.findRecientes(negocioA, bloqueA, null, 100)).isEmpty();
    }
}
