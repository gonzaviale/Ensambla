package com.ensambla.backend.domain.useCase.caja.cerrarCaja;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.caja.CierreCajaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CerrarCaja {

    private final NegocioRepository negocioRepository;
    private final SistemaRepository sistemaRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final UsoPlanRepository usoPlanRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CierreCajaItem execute(CerrarCajaRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return CierreCajaItem.de(transaccionPort.ejecutar(() -> cerrar(negocioId, request)));
    }

    private CierreCaja cerrar(NegocioId negocioId, CerrarCajaRequest request) {
        usoPlanRepository.bloquearNegocio(negocioId);
        var anterior = cierreCajaRepository.findByIdAndNegocioId(request.id(), negocioId);
        if (anterior.isPresent()) {
            if (!anterior.get().huella().equals(request.huella())) {
                throw new DuplicateResourceException("Este cierre ya se guardó con otros datos");
            }
            return anterior.get();
        }
        sistemaRepository.obtenerInventarioCaja(negocioId, request.proyectoId(), request.inventarioId());
        ResumenCaja resumen = ResumenCaja.de(ventaCajaRepository.listarAbiertas(
                negocioId, request.proyectoId(), request.inventarioId()));
        CierreCaja cierre = CierreCaja.nuevo(request.id(), negocioId, request.proyectoId(),
                request.inventarioId(), request.huella(), resumen, request.revision(), request.fondoInicial(),
                request.entradas(), request.retiros(), request.contado(), request.nota(), OffsetDateTime.now(clock));
        cierreCajaRepository.save(cierre);
        ventaCajaRepository.marcarCerradas(negocioId, request.id(), resumen.ventas().stream().map(venta -> venta.id()).toList());
        return cierre;
    }
}
