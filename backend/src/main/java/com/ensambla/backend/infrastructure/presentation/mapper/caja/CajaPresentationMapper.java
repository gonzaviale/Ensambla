package com.ensambla.backend.infrastructure.presentation.mapper.caja;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaRequest;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobrarVentaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobrarVentaWebResponse;
import org.springframework.stereotype.Component;

@Component
public class CajaPresentationMapper {

    public CobrarVentaRequest toRequest(UsuarioId usuarioId, CobrarVentaWebRequest request) {
        return new CobrarVentaRequest(usuarioId, request.id(), request.proyectoId(), request.inventarioId(),
                request.ventasId(), request.lineas().stream().map(linea -> new CobrarVentaRequest.LineaRequest(
                        linea.productoId(), linea.cantidad(), linea.precio(), linea.modo())).toList(),
                request.medio(), request.recibido());
    }

    public CobrarVentaWebResponse toResponse(CobrarVentaResponse response) {
        return new CobrarVentaWebResponse(response.id(), response.lineas().stream().map(linea ->
                new CobrarVentaWebResponse.LineaWebResponse(linea.productoId(), linea.nombre(), linea.modo(),
                        linea.cantidad(), linea.precio(), linea.importe())).toList(), response.medio(),
                response.total(), response.recibido(), response.vuelto(), response.creadoEn());
    }
}
