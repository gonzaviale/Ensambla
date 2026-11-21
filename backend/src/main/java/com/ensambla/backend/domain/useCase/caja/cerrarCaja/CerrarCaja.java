package com.ensambla.backend.domain.useCase.caja.cerrarCaja;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.caja.CierreCajaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
public class CerrarCaja {

    private final NegocioRepository negocioRepository;
    private final SistemaRepository sistemaRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final CobroCajaRepository cobroCajaRepository;
    private final UsoPlanRepository usoPlanRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CierreCajaItem execute(CerrarCajaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return CierreCajaItem.de(cerrarEnTransaccion(negocioId, request));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private CierreCaja cerrarEnTransaccion(NegocioId negocioId, CerrarCajaRequest request) {
        return transaccionPort.ejecutar(() -> cerrar(negocioId, request));
    }

    /** Un cierre repetido con los mismos datos devuelve el que ya estaba; con otros datos, se rechaza. */
    private CierreCaja cerrar(NegocioId negocioId, CerrarCajaRequest request) {
        bloquearNegocio(negocioId);
        Optional<CierreCaja> anterior = buscarCierre(request.id(), negocioId);
        if (anterior.isPresent()) {
            return repetirCierre(anterior.get(), request);
        }
        return cerrarLaCaja(negocioId, request);
    }

    private CierreCaja repetirCierre(CierreCaja anterior, CerrarCajaRequest request) {
        if (!anterior.huella().equals(request.huella())) {
            throw new DuplicateResourceException("Este cierre ya se guardó con otros datos");
        }
        return anterior;
    }

    private CierreCaja cerrarLaCaja(NegocioId negocioId, CerrarCajaRequest request) {
        exigirInventarioDeCaja(negocioId, request);
        ResumenCaja resumen = resumenAbierto(negocioId, request);
        CierreCaja cierre = cierreDe(negocioId, request, resumen);
        guardarCierre(cierre);
        marcarVentasCerradas(negocioId, request.id(), resumen);
        marcarCobrosCerrados(negocioId, request.id(), resumen);
        return cierre;
    }

    private ResumenCaja resumenAbierto(NegocioId negocioId, CerrarCajaRequest request) {
        return ResumenCaja.de(
                ventaCajaRepository.listarAbiertas(negocioId, request.proyectoId(), request.inventarioId()),
                cobroCajaRepository.listarAbiertos(negocioId, request.proyectoId(), request.inventarioId()));
    }

    private CierreCaja cierreDe(NegocioId negocioId, CerrarCajaRequest request, ResumenCaja resumen) {
        return CierreCaja.nuevo(request.id(), negocioId, request.proyectoId(),
                request.inventarioId(), request.huella(), resumen, request.revision(), request.fondoInicial(),
                request.entradas(), request.retiros(), request.contado(), request.nota(), OffsetDateTime.now(clock));
    }

    private void bloquearNegocio(NegocioId negocioId) {
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private Optional<CierreCaja> buscarCierre(CierreCajaId id, NegocioId negocioId) {
        return cierreCajaRepository.findByIdAndNegocioId(id, negocioId);
    }

    private void exigirInventarioDeCaja(NegocioId negocioId, CerrarCajaRequest request) {
        sistemaRepository.obtenerInventarioCaja(negocioId, request.proyectoId(), request.inventarioId());
    }

    private void guardarCierre(CierreCaja cierre) {
        cierreCajaRepository.save(cierre);
    }

    private void marcarVentasCerradas(NegocioId negocioId, CierreCajaId cierreId, ResumenCaja resumen) {
        ventaCajaRepository.marcarCerradas(negocioId, cierreId, resumen.ventas().stream().map(venta -> venta.id()).toList());
    }

    private void marcarCobrosCerrados(NegocioId negocioId, CierreCajaId cierreId, ResumenCaja resumen) {
        cobroCajaRepository.marcarCerrados(negocioId, cierreId, resumen.cobros().stream().map(cobro -> cobro.id()).toList());
    }
}
