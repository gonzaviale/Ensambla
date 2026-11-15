package com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.marca.KitDeMarcaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class GuardarKitDeMarca {

    private final NegocioRepository negocioRepository;

    private final KitDeMarcaRepository kitDeMarcaRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public KitDeMarcaItem execute(GuardarKitDeMarcaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return KitDeMarcaItem.de(personalizarGuardando(negocioId, request));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private KitDeMarca personalizarGuardando(NegocioId negocioId, GuardarKitDeMarcaRequest request) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return transaccionPort.ejecutar(() -> personalizar(negocioId, request, ahora));
    }

    private KitDeMarca personalizar(NegocioId negocioId, GuardarKitDeMarcaRequest request, OffsetDateTime ahora) {
        KitDeMarca kit = obtenerKitDelNegocio(negocioId, ahora);
        kit.personalizar(request.principal(), request.secundario(), request.tipografia(), request.tono(), ahora);
        return guardar(kit);
    }

    private KitDeMarca obtenerKitDelNegocio(NegocioId negocioId, OffsetDateTime ahora) {
        return kitDeMarcaRepository.obtenerOPredeterminado(negocioId, ahora);
    }

    private KitDeMarca guardar(KitDeMarca kit) {
        return kitDeMarcaRepository.save(kit);
    }
}
