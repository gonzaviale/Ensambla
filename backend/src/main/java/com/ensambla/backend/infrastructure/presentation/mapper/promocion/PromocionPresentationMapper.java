package com.ensambla.backend.infrastructure.presentation.mapper.promocion;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionRequest.ProductoARedactar;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocionResponse;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.CanalItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.ProductoPromocionadoItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.PromocionItem;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesRequest;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromocionesResponse;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.CanalResponse;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.ProductoPromocionWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.ProductoPromocionadoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.PromocionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.RedactarPromocionWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.SugerenciasDePromocionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.TextoDePromocionResponse;
import org.springframework.stereotype.Component;

@Component
public class PromocionPresentationMapper {

    public SugerirPromocionesRequest toSugerirRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
        return new SugerirPromocionesRequest(usuarioId, proyectoId);
    }

    public SugerenciasDePromocionResponse toResponse(SugerirPromocionesResponse response) {
        return new SugerenciasDePromocionResponse(
                response.promociones().stream().map(this::toResponse).toList(),
                response.canales().stream().map(this::toResponse).toList());
    }

    public RedactarPromocionRequest toRedactarRequest(UsuarioId usuarioId, RedactarPromocionWebRequest request) {
        return new RedactarPromocionRequest(
                usuarioId,
                request.canal(),
                request.clave(),
                request.titulo(),
                request.gancho(),
                request.productos().stream().map(this::toProducto).toList());
    }

    public TextoDePromocionResponse toResponse(RedactarPromocionResponse response) {
        return new TextoDePromocionResponse(response.texto());
    }

    private PromocionResponse toResponse(PromocionItem promocion) {
        return new PromocionResponse(
                promocion.clave(),
                promocion.titulo(),
                promocion.gancho(),
                promocion.productos().stream().map(this::toResponse).toList());
    }

    private ProductoPromocionadoResponse toResponse(ProductoPromocionadoItem producto) {
        return new ProductoPromocionadoResponse(producto.nombre(), producto.precio());
    }

    private CanalResponse toResponse(CanalItem canal) {
        return new CanalResponse(canal.clave(), canal.nombre());
    }

    private ProductoARedactar toProducto(ProductoPromocionWebRequest producto) {
        return new ProductoARedactar(producto.nombre(), producto.precio());
    }
}
