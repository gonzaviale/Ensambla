package com.ensambla.backend.domain.useCase.inventario;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.inventario.InventarioService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStock;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockRequest;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockResponse;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientos;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientosRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimiento;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoResponse;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioUseCasesTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_NOMBRE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NegocioService negocioService;

    @Mock
    private BloqueService bloqueService;

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private RegistroRepository registroRepository;

    private RegistrarMovimiento registrarMovimiento;
    private ListarMovimientos listarMovimientos;
    private ConsultarStock consultarStock;

    @BeforeEach
    void setUp() {
        InventarioService inventarioService = new InventarioService(movimientoStockRepository);
        registrarMovimiento = new RegistrarMovimiento(negocioService, bloqueService, campoService,
                registroDatosService, inventarioService, movimientoStockRepository, new TransaccionDirecta(), RELOJ);
        listarMovimientos = new ListarMovimientos(
                negocioService, bloqueService, campoService, registroRepository, movimientoStockRepository);
        consultarStock = new ConsultarStock(
                negocioService, bloqueService, campoService, registroDatosService, inventarioService);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Registrar guarda el movimiento del producto del bloque y devuelve el stock resultante")
    void registrarShouldSaveMovimientoAndReturnResultingStock() {
        givenBloqueConProducto(BLOQUE_ID);
        givenSaveReturnsArgument();
        givenStock("12");

        RegistrarMovimientoResponse response = registrarMovimiento.execute(new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "ENTRADA", new BigDecimal("2"), null, " Compra "));

        ArgumentCaptor<MovimientoStock> guardado = ArgumentCaptor.forClass(MovimientoStock.class);
        verify(movimientoStockRepository).save(guardado.capture());
        assertThat(guardado.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getValue().getFecha()).isEqualTo(LocalDate.parse("2026-10-13"));
        assertThat(guardado.getValue().getNota()).isEqualTo("Compra");
        assertThat(response.movimiento().producto()).isEqualTo("Harina");
        assertThat(response.movimiento().tipo()).isEqualTo("ENTRADA");
        assertThat(response.stockActual()).isEqualByComparingTo("12");
    }

    @Test
    @DisplayName("Registrar rechaza un movimiento inválido antes de buscar nada")
    void registrarShouldRejectInvalidMovimientoFirst() {
        RegistrarMovimientoRequest invalido = new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, null, "ENTRADA", BigDecimal.ONE, null, null);

        assertThatThrownBy(() -> registrarMovimiento.execute(invalido))
                .isInstanceOf(RegistroInvalidoException.class);
        verifyNoInteractions(bloqueService, registroDatosService, movimientoStockRepository);
    }

    @Test
    @DisplayName("Registrar responde 'no existe' si el producto es de otro bloque")
    void registrarShouldRejectProductOfAnotherBloque() {
        givenBloqueConProducto(OTRO_BLOQUE_ID);
        RegistrarMovimientoRequest request = new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "ENTRADA", BigDecimal.ONE, null, null);

        assertThatThrownBy(() -> registrarMovimiento.execute(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar devuelve los movimientos con el nombre de su producto")
    void listarShouldReturnMovimientosNamedAfterTheirProduct() {
        Bloque bloque = Bloque.builder().id(BLOQUE_ID).build();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque);
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(givenCampos());
        when(movimientoStockRepository.findRecientes(BLOQUE_ID, NEGOCIO_ID, PRODUCTO_ID, 100)).thenReturn(List.of(
                MovimientoStock.builder().registroId(PRODUCTO_ID).tipo(TipoMovimiento.SALIDA)
                        .cantidad(BigDecimal.ONE).build()));
        when(registroRepository.findAllByIdsAndBloque(List.of(PRODUCTO_ID), BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(givenProducto(BLOQUE_ID)));

        List<MovimientoItem> response = listarMovimientos.execute(
                new ListarMovimientosRequest(USUARIO_ID, BLOQUE_ID, PRODUCTO_ID));

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.producto()).isEqualTo("Harina");
            assertThat(item.tipo()).isEqualTo("SALIDA");
        });
    }

    @Test
    @DisplayName("Stock consulta una página de productos con la misma búsqueda que el listado y su stock")
    void stockShouldPairThePageOfProductsWithTheirStock() {
        Bloque bloque = Bloque.builder().id(BLOQUE_ID).build();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque);
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(givenCampos());
        when(registroDatosService.listar(any())).thenReturn(new Pagina<>(List.of(givenProducto(BLOQUE_ID)), 0, 20, 1));
        givenStock("4");

        ConsultarStockResponse response = consultarStock.execute(
                new ConsultarStockRequest(USUARIO_ID, BLOQUE_ID, "har", 0, 20));

        ArgumentCaptor<ConsultaRegistros> consulta = ArgumentCaptor.forClass(ConsultaRegistros.class);
        verify(registroDatosService).listar(consulta.capture());
        assertThat(consulta.getValue().texto()).isEqualTo("har");
        assertThat(consulta.getValue().negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(response.contenido()).singleElement().satisfies(item -> {
            assertThat(item.producto()).isEqualTo("Harina");
            assertThat(item.stock()).isEqualByComparingTo("4");
        });
    }

    @Test
    @DisplayName("Registrar una entrada la guarda sin consultar el stock antes")
    void registrarShouldSaveEntradaWithoutCheckingStockFirst() {
        givenBloqueConProducto(BLOQUE_ID);
        givenSaveReturnsArgument();
        givenStock("7");

        registrarMovimiento.execute(new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "ENTRADA", new BigDecimal("5"), null, null));

        InOrder orden = inOrder(movimientoStockRepository);
        orden.verify(movimientoStockRepository).save(any());
        orden.verify(movimientoStockRepository).stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID);
    }

    @Test
    @DisplayName("Registrar una salida menor o igual al stock la guarda")
    void registrarShouldSaveSalidaWhenStockIsEnough() {
        givenBloqueConProducto(BLOQUE_ID);
        givenSaveReturnsArgument();
        givenStock("5");

        RegistrarMovimientoResponse response = registrarMovimiento.execute(new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "SALIDA", new BigDecimal("5"), null, null));

        assertThat(response.movimiento().tipo()).isEqualTo("SALIDA");
    }

    @Test
    @DisplayName("Registrar una salida mayor al stock la rechaza y avisa cuánto queda")
    void registrarShouldRejectSalidaThatLeavesNegativeStock() {
        givenProductoDelBloque();
        givenStock("2.500");

        assertThatThrownBy(() -> registrarMovimiento.execute(new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "SALIDA", new BigDecimal("3"), null, null)))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsEntry("cantidad", "No hay stock suficiente: quedan 2.5"));
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registrar la salida de un producto sin movimientos la rechaza: no hay stock")
    void registrarShouldRejectSalidaOfProductWithoutMovements() {
        givenProductoDelBloque();
        when(movimientoStockRepository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID)).thenReturn(Map.of());

        assertThatThrownBy(() -> registrarMovimiento.execute(new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "SALIDA", BigDecimal.ONE, null, null)))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsEntry("cantidad", "No hay stock suficiente: quedan 0"));
    }

    private void givenStock(String stock) {
        when(movimientoStockRepository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID))
                .thenReturn(Map.of(PRODUCTO_ID, new BigDecimal(stock)));
    }

    private void givenSaveReturnsArgument() {
        when(movimientoStockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenProductoDelBloque() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(givenProducto(BLOQUE_ID));
    }

    private void givenBloqueConProducto(UUID bloqueDelProducto) {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(givenProducto(bloqueDelProducto));
        if (bloqueDelProducto.equals(BLOQUE_ID)) {
            when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(givenCampos());
        }
    }

    private static Registro givenProducto(UUID bloqueId) {
        return Registro.builder()
                .id(PRODUCTO_ID)
                .bloqueId(bloqueId)
                .valores(Map.of(CAMPO_NOMBRE_ID.toString(), "Harina"))
                .build();
    }

    private static List<Campo> givenCampos() {
        return List.of(Campo.builder().id(CAMPO_NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build());
    }
}
