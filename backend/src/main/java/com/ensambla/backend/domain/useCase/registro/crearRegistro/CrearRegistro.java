package com.ensambla.backend.domain.useCase.registro.crearRegistro;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearRegistro {

    private final NegocioService negocioService;
    private final BloqueService bloqueService;
    private final CampoService campoService;
    private final RegistroDatosService registroDatosService;
    private final ControlPlanService controlPlanService;
    private final RegistroRepository registroRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CrearRegistroResponse execute(CrearRegistroRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Registro creado = transaccionPort.ejecutar(
                () -> crear(negocioId, bloque.getId(), campos, request.valores(), ahora));
        return CrearRegistroResponse.de(creado.conFormulas(campos));
    }

    private Registro crear(UUID negocioId, UUID bloqueId, List<Campo> campos, Map<String, Object> valores,
                           OffsetDateTime ahora) {
        Map<String, Object> validos = Registro.validar(campos, valores);
        registroDatosService.validarConexiones(negocioId, campos, validos);
        controlPlanService.validarAltaRegistros(negocioId, 1, ahora);
        return registroRepository.save(Registro.nuevo(negocioId, bloqueId, validos, ahora));
    }
}
