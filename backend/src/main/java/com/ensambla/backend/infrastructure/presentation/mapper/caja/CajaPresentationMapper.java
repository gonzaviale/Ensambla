package com.ensambla.backend.infrastructure.presentation.mapper.caja;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.useCase.caja.ResumenCajaItem;
import com.ensambla.backend.domain.useCase.caja.CierreCajaItem;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaResponse;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierreRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CerrarCajaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CierreCajaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.ConsultarCajaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.ResumenCajaWebResponse;
import java.time.LocalDate;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaRequest;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobrarVentaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobrarVentaWebResponse;
import org.springframework.stereotype.Component;

@Component
public class CajaPresentationMapper {

    public ConsultarCajaRequest toConsulta(UsuarioId usuarioId, ProyectoId proyectoId, BloqueId inventarioId,
            LocalDate fecha, int pagina) {
        return new ConsultarCajaRequest(usuarioId, proyectoId, inventarioId, fecha, pagina);
    }

    public CerrarCajaRequest toCierre(UsuarioId usuarioId, CerrarCajaWebRequest request) {
        return new CerrarCajaRequest(usuarioId, request.id(), request.proyectoId(), request.inventarioId(),
                request.revision(), request.fondoInicial(), request.entradas(), request.retiros(), request.contado(), request.nota());
    }

    public ObtenerCierreRequest toObtenerCierre(UsuarioId usuarioId, CierreCajaId id) {
        return new ObtenerCierreRequest(usuarioId, id);
    }

    public ConsultarCajaWebResponse toConsultaResponse(ConsultarCajaResponse response) {
        return new ConsultarCajaWebResponse(response.fecha(), toResumen(response.dia()), toResumen(response.abierta()),
                response.cierres().stream().map(cierre -> new ConsultarCajaWebResponse.CierreListaWebResponse(
                        cierre.id(), cierre.desde(), cierre.cerradoEn(), cierre.total(),
                        cierre.cantidadVentas(), cierre.diferencia())).toList(), response.pagina(), response.hayMas());
    }

    public CierreCajaWebResponse toCierreResponse(CierreCajaItem cierre) {
        return new CierreCajaWebResponse(cierre.id(), toResumen(cierre.resumen()), cierre.fondoInicial(),
                cierre.entradas(), cierre.retiros(), cierre.esperado(), cierre.contado(), cierre.diferencia(),
                cierre.nota(), cierre.cerradoEn());
    }

    private ResumenCajaWebResponse toResumen(ResumenCajaItem resumen) {
        return new ResumenCajaWebResponse(resumen.ventas().stream().map(this::toResponse).toList(),
                resumen.total(), resumen.efectivo(), resumen.transferencia(), resumen.tarjeta(),
                resumen.desde(), resumen.revision());
    }

    public CobrarVentaRequest toRequest(UsuarioId usuarioId, CobrarVentaWebRequest request) {
        return new CobrarVentaRequest(usuarioId, request.id(), request.proyectoId(), request.inventarioId(),
                request.ventasId(), request.lineas().stream().map(linea -> new CobrarVentaRequest.LineaRequest(
                        linea.productoId(), linea.cantidad(), linea.precio(), linea.modo())).toList(),
                request.medio(), request.recibido(), request.pagos() == null ? null : request.pagos().stream()
                        .map(pago -> new CobrarVentaRequest.PagoRequest(pago.medio(), pago.importe())).toList());
    }

    public CobrarVentaWebResponse toResponse(CobrarVentaResponse response) {
        return new CobrarVentaWebResponse(response.id(), response.lineas().stream().map(linea ->
                new CobrarVentaWebResponse.LineaWebResponse(linea.productoId(), linea.nombre(), linea.modo(),
                        linea.cantidad(), linea.precio(), linea.importe())).toList(), response.medio(),
                response.total(), response.recibido(), response.vuelto(), response.creadoEn(),
                response.pagos().stream().map(pago ->
                        new CobrarVentaWebResponse.PagoWebResponse(pago.medio(), pago.importe())).toList());
    }
}
