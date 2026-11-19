package com.ensambla.backend.domain.useCase.registro.crearRegistro;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CrearRegistro {

    private final NegocioRepository negocioRepository;
    private final BloqueRepository bloqueRepository;
    private final CampoRepository campoRepository;
    private final RegistroDatosService registroDatosService;
    private final ControlPlanService controlPlanService;
    private final RegistroRepository registroRepository;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CrearRegistroResponse execute(CrearRegistroRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        List<Campo> campos = obtenerCamposDelBloque(bloque, negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Registro creado = crearEnTransaccion(negocioId, bloque.getId(), campos, request.valores(), ahora);
        return CrearRegistroResponse.de(creado.conFormulas(campos));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.obtener(bloqueId, negocioId);
    }

    private List<Campo> obtenerCamposDelBloque(Bloque bloque, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
    }

    private Registro crearEnTransaccion(NegocioId negocioId, BloqueId bloqueId, List<Campo> campos,
                                        Map<String, Object> valores, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> crear(negocioId, bloqueId, campos, valores, ahora));
    }

    private Registro crear(NegocioId negocioId, BloqueId bloqueId, List<Campo> campos, Map<String, Object> valores,
                           OffsetDateTime ahora) {
        Map<String, Object> validos = Registro.validar(campos, valores);
        validarConexiones(negocioId, campos, validos);
        exigirCupoDeAltas(negocioId, ahora);
        return guardar(Registro.nuevo(negocioId, bloqueId, validos, ahora));
    }

    private void validarConexiones(NegocioId negocioId, List<Campo> campos, Map<String, Object> valores) {
        registroDatosService.validarConexiones(negocioId, campos, valores);
    }

    private void exigirCupoDeAltas(NegocioId negocioId, OffsetDateTime ahora) {
        controlPlanService.validarAltaRegistros(negocioId, 1, ahora);
    }

    private Registro guardar(Registro registro) {
        return registroRepository.save(registro);
    }
}
