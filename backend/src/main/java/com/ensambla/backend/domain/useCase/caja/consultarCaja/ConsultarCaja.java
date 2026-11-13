package com.ensambla.backend.domain.useCase.caja.consultarCaja;

import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.port.TransaccionPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ConsultarCaja {

    private final NegocioRepository negocioRepository;
    private final SistemaRepository sistemaRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final UsoPlanRepository usoPlanRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public ConsultarCajaResponse execute(ConsultarCajaRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return transaccionPort.ejecutar(() -> consultar(negocioId, request));
    }

    private ConsultarCajaResponse consultar(NegocioId negocioId, ConsultarCajaRequest request) {
        usoPlanRepository.bloquearNegocio(negocioId);
        sistemaRepository.obtenerInventarioCaja(negocioId, request.proyectoId(), request.inventarioId());
        LocalDate fecha = request.fecha() == null ? ZonaNegocio.dia(OffsetDateTime.now(clock)) : request.fecha();
        var abiertas = ventaCajaRepository.listarAbiertas(negocioId, request.proyectoId(), request.inventarioId());
        var delDia = ventaCajaRepository.listarEntre(negocioId, request.proyectoId(), request.inventarioId(),
                ZonaNegocio.inicioDelDia(fecha), ZonaNegocio.inicioDelDia(fecha.plusDays(1)));
        var cierres = cierreCajaRepository.listar(negocioId, request.proyectoId(), request.inventarioId(), request.pagina());
        return ConsultarCajaResponse.de(fecha, ResumenCaja.de(delDia), ResumenCaja.de(abiertas), cierres, request.pagina());
    }
}
