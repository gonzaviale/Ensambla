package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.dto.aviso.ContadorAvisos;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AvisoService {

    private static final Comparator<Aviso> PENDIENTES_PRIMERO_Y_MAS_URGENTES = Comparator
            .comparing((Aviso aviso) -> !aviso.estaPendiente())
            .thenComparing(Comparator.comparing(Aviso::getPrioridad).reversed())
            .thenComparing(Comparator.comparing(Aviso::getCreadoEn).reversed());

    private final AvisoRepository avisoRepository;

    private final AccionAvisoService accionAvisoService;

    private final TransaccionPort transaccionPort;

    /** Sin estado, todos los avisos del negocio; los pendientes van primero, del más urgente al menos urgente. */
    public List<Aviso> listar(UUID negocioId, EstadoAviso estado) {
        List<Aviso> avisos = estado == null
                ? avisoRepository.findByNegocioId(negocioId)
                : avisoRepository.findByNegocioIdAndEstado(negocioId, estado);
        return avisos.stream().sorted(PENDIENTES_PRIMERO_Y_MAS_URGENTES).toList();
    }

    public ContadorAvisos contar(UUID negocioId) {
        List<Aviso> pendientes = avisoRepository.findByNegocioIdAndEstado(negocioId, EstadoAviso.PENDIENTE);
        return new ContadorAvisos(pendientes.size(), pendientes.stream().filter(Aviso::esUrgente).count());
    }

    public Aviso obtener(UUID avisoId, UUID negocioId) {
        return avisoRepository.findByIdAndNegocioId(avisoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El aviso no existe"));
    }

    /** Ejecuta la acción sugerida y marca el aviso como aplicado en una sola operación. */
    public Aviso aplicar(Aviso aviso, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            aviso.aplicar(ahora);
            accionAvisoService.ejecutar(aviso, ahora);
            return avisoRepository.save(aviso);
        });
    }

    public Aviso descartar(Aviso aviso, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            aviso.descartar(ahora);
            return avisoRepository.save(aviso);
        });
    }
}
