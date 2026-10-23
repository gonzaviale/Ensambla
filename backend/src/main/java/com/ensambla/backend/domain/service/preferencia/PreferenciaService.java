package com.ensambla.backend.domain.service.preferencia;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreferenciaService {

    private final PreferenciaRepository preferenciaRepository;

    /** Quien nunca guardó preferencias recibe las iniciales, sin que se escriba nada. */
    public PreferenciaUsuario obtener(UUID usuarioId, OffsetDateTime ahora) {
        return preferenciaRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> PreferenciaUsuario.inicial(usuarioId, ahora));
    }

    @Transactional
    public PreferenciaUsuario guardar(UUID usuarioId, List<String> descartadas, Map<String, Boolean> grupos,
                                      boolean bienvenidaVista, String nombre, OffsetDateTime ahora) {
        PreferenciaUsuario preferencia = obtener(usuarioId, ahora);
        preferencia.actualizar(descartadas, grupos, bienvenidaVista, nombre, ahora);
        return preferenciaRepository.save(preferencia);
    }
}
