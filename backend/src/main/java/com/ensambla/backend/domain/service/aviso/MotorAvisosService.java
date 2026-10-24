package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ejecuta todas las reglas registradas sobre un negocio y deja sus avisos al día: crea los nuevos, actualiza los que
 * cambiaron, vence los que ya no aplican y no vuelve a crear uno descartado o aplicado mientras sus datos de origen
 * (su huella) sigan iguales. Una regla nueva se suma registrándola como bean, sin tocar el motor.
 */
@RequiredArgsConstructor
public class MotorAvisosService {

    private final List<ReglaAviso> reglas;

    private final ContextoNegocioService contextoNegocioService;

    private final AvisoRepository avisoRepository;

    private final TransaccionPort transaccionPort;

    public void ejecutar(UUID negocioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            ContextoNegocio contexto = contextoNegocioService.armar(negocioId);
            reglas.forEach(regla -> sincronizar(negocioId, regla.tipo(), regla.evaluar(contexto), ahora));
        });
    }

    private void sincronizar(UUID negocioId, TipoAviso tipo, List<ResultadoRegla> resultados, OffsetDateTime ahora) {
        List<Aviso> pendientes = avisoRepository.findByNegocioIdAndTipoAndEstado(
                negocioId, tipo, EstadoAviso.PENDIENTE);
        Set<UUID> vigentes = new HashSet<>();
        for (ResultadoRegla resultado : resultados) {
            Optional<Aviso> existente = avisoRepository.findByNegocioIdAndHuella(
                    negocioId, resultado.huellaPara(tipo));
            if (existente.isPresent()) {
                if (existente.get().estaPendiente() || existente.get().getEstado() == EstadoAviso.VENCIDO) {
                    vencerOtrosPendientesDe(resultado.entidad(), existente.get(), pendientes, ahora);
                    vigentes.add(avisoRepository.save(reactivar(existente.get(), resultado, ahora)).getId());
                }
                continue;
            }
            Aviso aviso = pendienteDe(resultado.entidad(), pendientes)
                    .map(pendiente -> reactivar(pendiente, resultado, ahora))
                    .orElseGet(() -> Aviso.nuevo(negocioId, tipo, resultado, ahora));
            vigentes.add(avisoRepository.save(aviso).getId());
        }
        pendientes.stream()
                .filter(pendiente -> !vigentes.contains(pendiente.getId()))
                .filter(Aviso::estaPendiente)
                .forEach(pendiente -> avisoRepository.save(vencida(pendiente, ahora)));
    }

    private void vencerOtrosPendientesDe(String entidad, Aviso conservado, List<Aviso> pendientes,
                                         OffsetDateTime ahora) {
        pendientes.stream()
                .filter(pendiente -> pendiente.getEntidad().equals(entidad))
                .filter(pendiente -> !pendiente.getId().equals(conservado.getId()))
                .forEach(pendiente -> avisoRepository.save(vencida(pendiente, ahora)));
    }

    private static Optional<Aviso> pendienteDe(String entidad, List<Aviso> pendientes) {
        return pendientes.stream().filter(pendiente -> pendiente.getEntidad().equals(entidad)).findFirst();
    }

    private static Aviso reactivar(Aviso aviso, ResultadoRegla resultado, OffsetDateTime ahora) {
        aviso.actualizarCon(resultado, ahora);
        return aviso;
    }

    private static Aviso vencida(Aviso aviso, OffsetDateTime ahora) {
        aviso.vencer(ahora);
        return aviso;
    }
}
