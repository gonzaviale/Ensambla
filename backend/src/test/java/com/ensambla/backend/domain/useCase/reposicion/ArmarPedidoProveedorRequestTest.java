package com.ensambla.backend.domain.useCase.reposicion;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest.LineaDePedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.ReposicionDePrueba.FIDEOS_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.PROYECTO_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ArmarPedidoProveedorRequestTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("Junta la cantidad de cada producto en el orden en que llegaron")
    void cantidadesPorProductoShouldKeepTheOrder() {
        ArmarPedidoProveedorRequest request = pedidoCon(linea(YERBA_ID, "12"), linea(FIDEOS_ID, "5"));

        Map<RegistroId, BigDecimal> result = request.cantidadesPorProducto();

        assertThat(result).containsExactly(
                Map.entry(YERBA_ID, new BigDecimal("12")), Map.entry(FIDEOS_ID, new BigDecimal("5")));
    }

    @Test
    @DisplayName("Si un producto se repite vale la última cantidad")
    void cantidadesPorProductoShouldKeepTheLastQuantityOfARepeatedProduct() {
        ArmarPedidoProveedorRequest request = pedidoCon(linea(YERBA_ID, "12"), linea(YERBA_ID, "20"));

        Map<RegistroId, BigDecimal> result = request.cantidadesPorProducto();

        assertThat(result).containsExactly(Map.entry(YERBA_ID, new BigDecimal("20")));
    }

    // --- arrange ---
    private ArmarPedidoProveedorRequest pedidoCon(LineaDePedido... lineas) {
        return new ArmarPedidoProveedorRequest(USUARIO_ID, PROYECTO_ID, "Molinos", List.of(lineas));
    }

    private LineaDePedido linea(RegistroId productoId, String cantidad) {
        return new LineaDePedido(productoId, new BigDecimal(cantidad));
    }
}
