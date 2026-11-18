package com.ensambla.backend.domain.useCase.caja.registrarCobro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RegistrarCobro {

    private final NegocioRepository negocioRepository;
    private final UsoPlanRepository usoPlanRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final CobroCajaRepository cobroCajaRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CobroCajaItem execute(RegistrarCobroRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return CobroCajaItem.de(transaccionPort.ejecutar(() -> cobrar(negocioId, request)));
    }

    private CobroCaja cobrar(NegocioId negocioId, RegistrarCobroRequest request) {
        usoPlanRepository.bloquearNegocio(negocioId);
        var anterior = cobroCajaRepository.findByIdAndNegocioId(request.id(), negocioId);
        if (anterior.isPresent()) {
            if (!anterior.get().huella().equals(request.huella())) {
                throw new DuplicateResourceException("Este cobro ya se registró con otros datos");
            }
            return anterior.get();
        }
        List<CobroCaja.Aplicacion> aplicaciones = request.aplicaciones().stream().map(aplicacion ->
                new CobroCaja.Aplicacion(aplicacion.ventaId(), aplicacion.pagos().stream().map(pago ->
                        new PagoCaja(MedioPagoCaja.valueOf(pago.medio()), pago.importe())).toList())).toList();
        Map<VentaCajaId, VentaCaja> ventas = ventaCajaRepository.listarPorIds(negocioId,
                aplicaciones.stream().map(CobroCaja.Aplicacion::ventaId).toList()).stream()
                .collect(Collectors.toMap(VentaCaja::id, Function.identity()));
        if (aplicaciones.isEmpty()) {
            throw new BusinessRuleException("Elegí al menos una deuda para cobrar");
        }
        VentaCaja primera = obtenerVenta(ventas, aplicaciones.getFirst().ventaId());
        if (primera.clienteId() == null) {
            throw new BusinessRuleException("La venta no tiene una deuda a nombre de un cliente");
        }
        CobroCaja cobro = new CobroCaja(request.id(), negocioId, request.proyectoId(), request.inventarioId(),
                primera.clienteId(), primera.clienteNombre(), request.huella(), aplicaciones, OffsetDateTime.now(clock));
        for (CobroCaja.Aplicacion aplicacion : aplicaciones) {
            VentaCaja venta = obtenerVenta(ventas, aplicacion.ventaId());
            validarDestino(venta, cobro);
            ventaCajaRepository.actualizarCobros(venta.conCobro(aplicacion.pagos()), cobro.creadoEn());
        }
        return cobroCajaRepository.save(cobro);
    }

    private VentaCaja obtenerVenta(Map<VentaCajaId, VentaCaja> ventas, VentaCajaId id) {
        VentaCaja venta = ventas.get(id);
        if (venta == null) {
            throw new ResourceNotFoundException("Venta no encontrada en el negocio");
        }
        return venta;
    }

    private void validarDestino(VentaCaja venta, CobroCaja cobro) {
        if (!venta.proyectoId().equals(cobro.proyectoId()) || !venta.inventarioId().equals(cobro.inventarioId())) {
            throw new ResourceNotFoundException("Venta no encontrada en esta caja");
        }
        if (!cobro.clienteId().equals(venta.clienteId())) {
            throw new BusinessRuleException("Elegí deudas del mismo cliente para registrar un pago");
        }
    }
}
