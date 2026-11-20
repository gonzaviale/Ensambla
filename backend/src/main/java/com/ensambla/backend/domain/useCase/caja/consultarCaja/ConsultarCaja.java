package com.ensambla.backend.domain.useCase.caja.consultarCaja;

import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.port.TransaccionPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ConsultarCaja {

    private final NegocioRepository negocioRepository;
    private final SistemaRepository sistemaRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final CobroCajaRepository cobroCajaRepository;
    private final UsoPlanRepository usoPlanRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public ConsultarCajaResponse execute(ConsultarCajaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return consultarEnTransaccion(negocioId, request);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private ConsultarCajaResponse consultarEnTransaccion(NegocioId negocioId, ConsultarCajaRequest request) {
        return transaccionPort.ejecutar(() -> consultar(negocioId, request));
    }

    private ConsultarCajaResponse consultar(NegocioId negocioId, ConsultarCajaRequest request) {
        bloquearNegocio(negocioId);
        exigirInventarioDeCaja(negocioId, request);
        LocalDate fecha = fechaConsultada(request);
        ResumenCaja abierta = resumenAbierto(negocioId, request);
        ResumenCaja delDia = resumenDelDia(negocioId, request, fecha);
        List<CierreCaja> cierres = listarCierres(negocioId, request);
        return ConsultarCajaResponse.de(fecha, delDia, abierta, cierres, request.pagina());
    }

    private LocalDate fechaConsultada(ConsultarCajaRequest request) {
        return request.fecha() == null ? ZonaNegocio.dia(OffsetDateTime.now(clock)) : request.fecha();
    }

    private ResumenCaja resumenAbierto(NegocioId negocioId, ConsultarCajaRequest request) {
        return ResumenCaja.de(
                ventaCajaRepository.listarAbiertas(negocioId, request.proyectoId(), request.inventarioId()),
                cobroCajaRepository.listarAbiertos(negocioId, request.proyectoId(), request.inventarioId()));
    }

    private ResumenCaja resumenDelDia(NegocioId negocioId, ConsultarCajaRequest request, LocalDate fecha) {
        OffsetDateTime desde = ZonaNegocio.inicioDelDia(fecha);
        OffsetDateTime hasta = ZonaNegocio.inicioDelDia(fecha.plusDays(1));
        return ResumenCaja.de(
                ventaCajaRepository.listarEntre(negocioId, request.proyectoId(), request.inventarioId(), desde, hasta),
                cobroCajaRepository.listarEntre(negocioId, request.proyectoId(), request.inventarioId(), desde, hasta));
    }

    private void bloquearNegocio(NegocioId negocioId) {
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private void exigirInventarioDeCaja(NegocioId negocioId, ConsultarCajaRequest request) {
        sistemaRepository.obtenerInventarioCaja(negocioId, request.proyectoId(), request.inventarioId());
    }

    private List<CierreCaja> listarCierres(NegocioId negocioId, ConsultarCajaRequest request) {
        return cierreCajaRepository.listar(negocioId, request.proyectoId(), request.inventarioId(), request.pagina());
    }
}
