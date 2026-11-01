package com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias;

import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.useCase.preferencia.PreferenciasItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class GuardarPreferencias {

    private final PreferenciaRepository preferenciaRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public PreferenciasItem execute(GuardarPreferenciasRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return PreferenciasItem.de(transaccionPort.ejecutar(() -> guardar(request, ahora)));
    }

    private PreferenciaUsuario guardar(GuardarPreferenciasRequest request, OffsetDateTime ahora) {
        PreferenciaUsuario preferencia = preferenciaRepository.obtenerOInicial(request.usuarioId(), ahora);
        preferencia.actualizar(
                request.descartadas(), request.grupos(), request.bienvenidaVista(), request.nombre(), ahora);
        return preferenciaRepository.save(preferencia);
    }
}
