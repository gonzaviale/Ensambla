package com.ensambla.backend.infrastructure.presentation.mapper.reposicion;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionRequest;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionResponse;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.GrupoDeProveedorItem;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ProductoFaltanteItem;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.LineaPedidoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.PedidoProveedorWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.ReposicionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.ReposicionDePrueba.PROYECTO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ReposicionPresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");

    private final ReposicionPresentationMapper mapper = new ReposicionPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de consulta con el usuario de la sesión y el sistema")
    void toConsultarRequestShouldCarryTheUserAndTheProject() {
        ConsultarReposicionRequest result = mapper.toConsultarRequest(USUARIO_ID, PROYECTO_ID);

        assertThat(result).isEqualTo(new ConsultarReposicionRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("Copia cada grupo y cada producto de la respuesta")
    void toResponseShouldCopyGroupsAndProducts() {
        ConsultarReposicionResponse response = new ConsultarReposicionResponse(List.of(new GrupoDeProveedorItem(
                "Molinos", List.of(new ProductoFaltanteItem(
                        YERBA_ID, "Yerba", new BigDecimal("3"), new BigDecimal("17"), "Quedan 3 (mínimo 10)")))));

        ReposicionResponse result = mapper.toResponse(response);

        assertThat(result.grupos()).singleElement().satisfies(grupo -> {
            assertThat(grupo.proveedor()).isEqualTo("Molinos");
            assertThat(grupo.productos()).singleElement().satisfies(producto -> {
                assertThat(producto.id()).isEqualTo(YERBA_ID);
                assertThat(producto.nombre()).isEqualTo("Yerba");
                assertThat(producto.cantidadActual()).isEqualByComparingTo("3");
                assertThat(producto.cantidadAPedir()).isEqualByComparingTo("17");
                assertThat(producto.situacion()).isEqualTo("Quedan 3 (mínimo 10)");
            });
        });
    }

    @Test
    @DisplayName("La respuesta sin grupos queda vacía")
    void toResponseShouldReturnNoGroupsWhenThereAreNone() {
        ReposicionResponse result = mapper.toResponse(new ConsultarReposicionResponse(List.of()));

        assertThat(result.grupos()).isEmpty();
    }

    @Test
    @DisplayName("Arma el pedido del PDF con el proveedor y cada línea elegida")
    void toArmarRequestShouldCarryTheSupplierAndTheLines() {
        PedidoProveedorWebRequest request = new PedidoProveedorWebRequest(
                "Molinos", List.of(new LineaPedidoWebRequest(YERBA_ID, new BigDecimal("12"))));

        ArmarPedidoProveedorRequest result = mapper.toArmarRequest(USUARIO_ID, PROYECTO_ID, request);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.proyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(result.proveedor()).isEqualTo("Molinos");
        assertThat(result.productos()).containsExactly(
                new ArmarPedidoProveedorRequest.LineaDePedido(YERBA_ID, new BigDecimal("12")));
    }
}
