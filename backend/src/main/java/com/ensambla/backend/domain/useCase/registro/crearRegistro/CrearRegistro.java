package com.ensambla.backend.domain.useCase.registro.crearRegistro;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearRegistro {

    private final NegocioService negocioService;
    private final BloqueService bloqueService;
    private final CampoService campoService;
    private final RegistroDatosService registroDatosService;
    private final Clock clock;

    public CrearRegistroResponse execute(CrearRegistroRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        Registro creado = registroDatosService.crear(
                negocioId, bloque.getId(), campos, request.valores(), OffsetDateTime.now(clock));
        return CrearRegistroResponse.de(creado.conFormulas(campos));
    }
}
