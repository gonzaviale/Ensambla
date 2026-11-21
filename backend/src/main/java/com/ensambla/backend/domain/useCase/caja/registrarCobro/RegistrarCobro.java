package com.ensambla.backend.domain.useCase.caja.registrarCobro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
import java.util.Optional;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return CobroCajaItem.de(cobrarEnTransaccion(negocioId, request));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private CobroCaja cobrarEnTransaccion(NegocioId negocioId, RegistrarCobroRequest request) {
        return transaccionPort.ejecutar(() -> cobrar(negocioId, request));
    }

    /** Un cobro repetido con los mismos datos devuelve el que ya estaba; con otros datos, se rechaza. */
    private CobroCaja cobrar(NegocioId negocioId, RegistrarCobroRequest request) {
        bloquearNegocio(negocioId);
        Optional<CobroCaja> anterior = buscarCobro(request.id(), negocioId);
        if (anterior.isPresent()) {
            return repetirCobro(anterior.get(), request);
        }
        return registrarCobroNuevo(negocioId, request);
    }

    private CobroCaja repetirCobro(CobroCaja anterior, RegistrarCobroRequest request) {
        if (!anterior.huella().equals(request.huella())) {
            throw new DuplicateResourceException("Este cobro ya se registró con otros datos");
        }
        return anterior;
    }

    private CobroCaja registrarCobroNuevo(NegocioId negocioId, RegistrarCobroRequest request) {
        List<CobroCaja.Aplicacion> aplicaciones = aplicacionesDe(request);
        Map<VentaCajaId, VentaCaja> ventas = obtenerVentas(negocioId, aplicaciones);
        exigirAlMenosUnaDeuda(aplicaciones);
        VentaCaja primera = obtenerVenta(ventas, aplicaciones.getFirst().ventaId());
        exigirDeudaDeUnCliente(primera);
        CobroCaja cobro = cobroDe(negocioId, request, aplicaciones, primera);
        aplicaciones.forEach(aplicacion -> cobrarLaVenta(obtenerVenta(ventas, aplicacion.ventaId()), aplicacion, cobro));
        return guardarCobro(cobro);
    }

    private List<CobroCaja.Aplicacion> aplicacionesDe(RegistrarCobroRequest request) {
        return request.aplicaciones().stream()
                .map(aplicacion -> new CobroCaja.Aplicacion(aplicacion.ventaId(), pagosDe(aplicacion)))
                .toList();
    }

    private List<PagoCaja> pagosDe(RegistrarCobroRequest.AplicacionRequest aplicacion) {
        return aplicacion.pagos().stream()
                .map(pago -> new PagoCaja(MedioPagoCaja.valueOf(pago.medio()), pago.importe()))
                .toList();
    }

    private Map<VentaCajaId, VentaCaja> obtenerVentas(NegocioId negocioId, List<CobroCaja.Aplicacion> aplicaciones) {
        List<VentaCajaId> ids = aplicaciones.stream().map(CobroCaja.Aplicacion::ventaId).toList();
        return listarVentas(negocioId, ids).stream().collect(Collectors.toMap(VentaCaja::id, Function.identity()));
    }

    private void exigirAlMenosUnaDeuda(List<CobroCaja.Aplicacion> aplicaciones) {
        if (aplicaciones.isEmpty()) {
            throw new BusinessRuleException("Elegí al menos una deuda para cobrar");
        }
    }

    private void exigirDeudaDeUnCliente(VentaCaja venta) {
        if (venta.clienteId() == null) {
            throw new BusinessRuleException("La venta no tiene una deuda a nombre de un cliente");
        }
    }

    private CobroCaja cobroDe(NegocioId negocioId, RegistrarCobroRequest request,
                              List<CobroCaja.Aplicacion> aplicaciones, VentaCaja primera) {
        return new CobroCaja(request.id(), negocioId, request.proyectoId(), request.inventarioId(),
                primera.clienteId(), primera.clienteNombre(), request.huella(), aplicaciones, OffsetDateTime.now(clock));
    }

    private void cobrarLaVenta(VentaCaja venta, CobroCaja.Aplicacion aplicacion, CobroCaja cobro) {
        validarDestino(venta, cobro);
        actualizarCobros(venta.conCobro(aplicacion.pagos()), cobro.creadoEn());
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

    private void bloquearNegocio(NegocioId negocioId) {
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private Optional<CobroCaja> buscarCobro(CobroCajaId id, NegocioId negocioId) {
        return cobroCajaRepository.findByIdAndNegocioId(id, negocioId);
    }

    private List<VentaCaja> listarVentas(NegocioId negocioId, List<VentaCajaId> ids) {
        return ventaCajaRepository.listarPorIds(negocioId, ids);
    }

    private void actualizarCobros(VentaCaja venta, OffsetDateTime ahora) {
        ventaCajaRepository.actualizarCobros(venta, ahora);
    }

    private CobroCaja guardarCobro(CobroCaja cobro) {
        return cobroCajaRepository.save(cobro);
    }
}
