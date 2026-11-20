package com.ensambla.backend.domain.useCase.registro.eliminarRegistro;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EliminarRegistro {

    private final NegocioRepository negocioRepository;
    private final RegistroRepository registroRepository;
    private final TransaccionPort transaccionPort;

    public void execute(EliminarRegistroRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Registro registro = obtenerRegistro(request.registroId(), negocioId);
        eliminarEnTransaccion(registro);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Registro obtenerRegistro(RegistroId registroId, NegocioId negocioId) {
        return registroRepository.obtener(registroId, negocioId);
    }

    private void eliminarEnTransaccion(Registro registro) {
        transaccionPort.ejecutar(() -> registroRepository.deleteById(registro.getId()));
    }
}
