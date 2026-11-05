package com.ensambla.backend.infrastructure.presentation.mapper.reposicion;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorRequest.LineaDePedido;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionRequest;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicionResponse;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.GrupoDeProveedorItem;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ProductoFaltanteItem;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.GrupoReposicionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.LineaPedidoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.PedidoProveedorWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.ProductoFaltanteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.ReposicionResponse;
import org.springframework.stereotype.Component;

@Component
public class ReposicionPresentationMapper {

    public ConsultarReposicionRequest toConsultarRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
        return new ConsultarReposicionRequest(usuarioId, proyectoId);
    }

    public ReposicionResponse toResponse(ConsultarReposicionResponse response) {
        return new ReposicionResponse(response.grupos().stream().map(this::toResponse).toList());
    }

    public ArmarPedidoProveedorRequest toArmarRequest(UsuarioId usuarioId, ProyectoId proyectoId,
                                                      PedidoProveedorWebRequest request) {
        return new ArmarPedidoProveedorRequest(
                usuarioId,
                proyectoId,
                request.proveedor(),
                request.productos().stream().map(this::toLinea).toList());
    }

    private GrupoReposicionResponse toResponse(GrupoDeProveedorItem grupo) {
        return new GrupoReposicionResponse(grupo.proveedor(), grupo.productos().stream().map(this::toResponse).toList());
    }

    private ProductoFaltanteResponse toResponse(ProductoFaltanteItem producto) {
        return new ProductoFaltanteResponse(
                producto.id(),
                producto.nombre(),
                producto.cantidadActual(),
                producto.cantidadAPedir(),
                producto.situacion());
    }

    private LineaDePedido toLinea(LineaPedidoWebRequest linea) {
        return new LineaDePedido(linea.productoId(), linea.cantidad());
    }
}
