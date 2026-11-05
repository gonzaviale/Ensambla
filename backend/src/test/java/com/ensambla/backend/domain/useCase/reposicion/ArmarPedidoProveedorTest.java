package com.ensambla.backend.domain.useCase.reposicion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reposicion.Faltantes;
import com.ensambla.backend.domain.model.reposicion.PedidoReposicion;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.GeneradorPedidoPdfPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.reposicion.ReposicionService;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedor;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest.LineaDePedido;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.ReposicionDePrueba.FIDEOS_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.PROYECTO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.datosCon;
import static com.ensambla.backend.support.ReposicionDePrueba.inventarioCon;
import static com.ensambla.backend.support.ReposicionDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmarPedidoProveedorTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    /** En UTC ya es el 20, pero en Argentina todavía es el 19. */
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ReposicionService reposicionService = mock(ReposicionService.class);
    private final GeneradorPedidoPdfPort generador = mock(GeneradorPedidoPdfPort.class);

    private ArmarPedidoProveedor armarPedidoProveedor;

    @BeforeEach
    void setUp() {
        armarPedidoProveedor = new ArmarPedidoProveedor(
                negocioRepository, reposicionService, generador, Clock.fixed(INSTANTE, ZoneOffset.UTC));
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
        when(generador.generar(any(PedidoReposicion.class))).thenReturn(PDF);
    }

    @Test
    @DisplayName("Devuelve el PDF con el nombre del proveedor y la fecha argentina")
    void executeShouldReturnThePdfNamedAfterTheSupplier() {
        givenFaltantesDelSistema();

        ArmarPedidoProveedorResponse result = whenArma("Molinos", pedidoDe(YERBA_ID, "12"));

        assertThat(result.contenido()).isEqualTo(PDF);
        assertThat(result.nombreArchivo()).isEqualTo("pedido-molinos-20261019.pdf");
    }

    @Test
    @DisplayName("Le pasa al generador el negocio, el proveedor, el día de hoy y las cantidades elegidas")
    void executeShouldPassTheChosenQuantitiesToTheGenerator() {
        givenFaltantesDelSistema();

        whenArma("Molinos", pedidoDe(YERBA_ID, "12"));

        PedidoReposicion pedido = pedidoGenerado();
        assertThat(pedido.negocio()).isEqualTo("Almacén de Ana");
        assertThat(pedido.proveedor()).isEqualTo("Molinos");
        assertThat(pedido.fecha()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(pedido.items()).singleElement().satisfies(item -> {
            assertThat(item.productoId()).isEqualTo(YERBA_ID);
            assertThat(item.cantidadAPedir()).isEqualByComparingTo("12");
        });
    }

    @Test
    @DisplayName("Rechaza un producto que ya no hace falta reponer sin generar el PDF")
    void executeShouldRejectAProductThatIsNoLongerMissing() {
        givenFaltantesDelSistema();

        assertThatThrownBy(() -> whenArma("Molinos", pedidoDe(FIDEOS_ID, "5")))
                .isInstanceOf(BusinessRuleException.class);
        verify(generador, never()).generar(any());
    }

    @Test
    @DisplayName("Rechaza un pedido sin productos")
    void executeShouldRejectAnOrderWithoutProducts() {
        givenFaltantesDelSistema();

        assertThatThrownBy(() -> whenArma("Molinos", List.of())).isInstanceOf(BusinessRuleException.class);
        verify(generador, never()).generar(any());
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void executeShouldNotFindAForeignProject() {
        when(reposicionService.faltantesDe(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> whenArma("Molinos", pedidoDe(YERBA_ID, "12")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(generador, never()).generar(any());
    }

    // --- arrange ---
    private void givenFaltantesDelSistema() {
        Faltantes faltantes = Faltantes.de(datosCon(inventarioCon(
                producto(YERBA_ID, "Yerba", 3, 10, "Molinos"),
                producto(FIDEOS_ID, "Fideos", 30, 10, "Molinos"))));
        when(reposicionService.faltantesDe(NEGOCIO_ID, PROYECTO_ID)).thenReturn(faltantes);
    }

    private static List<LineaDePedido> pedidoDe(RegistroId productoId, String cantidad) {
        return List.of(new LineaDePedido(productoId, new BigDecimal(cantidad)));
    }

    // --- act ---
    private ArmarPedidoProveedorResponse whenArma(String proveedor, List<LineaDePedido> productos) {
        return armarPedidoProveedor.execute(
                new ArmarPedidoProveedorRequest(USUARIO_ID, PROYECTO_ID, proveedor, productos));
    }

    private PedidoReposicion pedidoGenerado() {
        ArgumentCaptor<PedidoReposicion> captor = ArgumentCaptor.forClass(PedidoReposicion.class);
        verify(generador).generar(captor.capture());
        return captor.getValue();
    }
}
