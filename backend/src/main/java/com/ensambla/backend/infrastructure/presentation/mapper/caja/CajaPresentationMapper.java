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
import com.ensambla.backend.domain.useCase.caja.listarVentas.CuentaClienteCajaItem;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CuentaClienteCajaWebResponse;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCajaRequest;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCajaResponse;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobroRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobroCajaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.ListarVentasCajaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.RegistrarCobroWebRequest;

@Component
public class CajaPresentationMapper {

    public ListarVentasCajaRequest toListado(UsuarioId usuarioId, ProyectoId proyectoId, BloqueId inventarioId) {
        return new ListarVentasCajaRequest(usuarioId, proyectoId, inventarioId);
    }

    public ListarVentasCajaWebResponse toListadoResponse(ListarVentasCajaResponse response) {
        return new ListarVentasCajaWebResponse(response.ventas().stream().map(this::toResponse).toList(),
                response.cobros().stream().map(this::toCobroResponse).toList(),
                response.cuentas().stream().map(this::toCuentaResponse).toList());
    }

    private CuentaClienteCajaWebResponse toCuentaResponse(CuentaClienteCajaItem cuenta) {
        return new CuentaClienteCajaWebResponse(cuenta.clienteId(), cuenta.clienteNombre(), cuenta.saldo(),
                cuenta.cantidadPendientes(), cuenta.pendienteDesde(),
                cuenta.ventas().stream().map(this::toResponse).toList(),
                cuenta.cobros().stream().map(this::toCobroResponse).toList());
    }

    public RegistrarCobroRequest toCobro(UsuarioId usuarioId, RegistrarCobroWebRequest request) {
        return new RegistrarCobroRequest(usuarioId, request.id(), request.proyectoId(), request.inventarioId(),
                request.aplicaciones().stream().map(aplicacion -> new RegistrarCobroRequest.AplicacionRequest(
                        aplicacion.ventaId(), aplicacion.pagos().stream().map(pago ->
                                new RegistrarCobroRequest.PagoRequest(pago.medio(), pago.importe())).toList())).toList());
    }

    public CobroCajaWebResponse toCobroResponse(CobroCajaItem cobro) {
        return new CobroCajaWebResponse(cobro.id(), cobro.clienteId(), cobro.clienteNombre(), cobro.aplicaciones().stream().map(aplicacion ->
                new CobroCajaWebResponse.AplicacionWebResponse(aplicacion.ventaId(), aplicacion.pagos().stream().map(pago ->
                        new CobroCajaWebResponse.PagoWebResponse(pago.medio(), pago.importe())).toList(),
                        aplicacion.importe())).toList(), cobro.importe(), cobro.creadoEn());
    }

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
                        cierre.cantidadVentas(), cierre.diferencia(), cierre.cobrado())).toList(), response.pagina(), response.hayMas());
    }

    public CierreCajaWebResponse toCierreResponse(CierreCajaItem cierre) {
        return new CierreCajaWebResponse(cierre.id(), toResumen(cierre.resumen()), cierre.fondoInicial(),
                cierre.entradas(), cierre.retiros(), cierre.esperado(), cierre.contado(), cierre.diferencia(),
                cierre.nota(), cierre.cerradoEn());
    }

    private ResumenCajaWebResponse toResumen(ResumenCajaItem resumen) {
        return new ResumenCajaWebResponse(resumen.ventas().stream().map(this::toResponse).toList(),
                resumen.total(), resumen.efectivo(), resumen.transferencia(), resumen.tarjeta(),
                resumen.desde(), resumen.revision(), resumen.cobros().stream().map(this::toCobroResponse).toList());
    }

    public CobrarVentaRequest toRequest(UsuarioId usuarioId, CobrarVentaWebRequest request) {
        return new CobrarVentaRequest(usuarioId, request.id(), request.proyectoId(), request.inventarioId(),
                request.ventasId(), request.lineas().stream().map(linea -> new CobrarVentaRequest.LineaRequest(
                        linea.productoId(), linea.cantidad(), linea.precio(), linea.modo(), linea.descripcion())).toList(),
                request.medio(), request.recibido(), request.pagos() == null ? null : request.pagos().stream()
                        .map(pago -> new CobrarVentaRequest.PagoRequest(pago.medio(), pago.importe())).toList(),
                request.clienteId());
    }

    public CobrarVentaWebResponse toResponse(CobrarVentaResponse response) {
        return new CobrarVentaWebResponse(response.id(), response.lineas().stream().map(linea ->
                new CobrarVentaWebResponse.LineaWebResponse(linea.productoId(), linea.nombre(), linea.modo(),
                        linea.cantidad(), linea.precio(), linea.importe())).toList(), response.medio(),
                response.total(), response.recibido(), response.vuelto(), response.creadoEn(),
                response.pagos().stream().map(pago ->
                        new CobrarVentaWebResponse.PagoWebResponse(pago.medio(), pago.importe())).toList(),
                response.clienteId(), response.clienteNombre(), response.cobrado(), response.saldo(), response.estado(),
                response.pagosIniciales().stream().map(pago ->
                        new CobrarVentaWebResponse.PagoWebResponse(pago.medio(), pago.importe())).toList());
    }
}
