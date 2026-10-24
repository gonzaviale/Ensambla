package com.ensambla.backend.domain.useCase.registro.eliminarRegistro;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class EliminarRegistro {

    private final NegocioService negocioService;
    private final RegistroDatosService registroDatosService;
    private final RegistroRepository registroRepository;
    private final TransaccionPort transaccionPort;

    public void execute(EliminarRegistroRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro registro = registroDatosService.obtener(request.registroId(), negocioId);
        transaccionPort.ejecutar(() -> registroRepository.deleteById(registro.getId()));
    }
}
