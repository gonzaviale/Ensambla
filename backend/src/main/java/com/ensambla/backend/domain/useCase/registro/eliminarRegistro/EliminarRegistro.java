package com.ensambla.backend.domain.useCase.registro.eliminarRegistro;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Registro registro = registroRepository.obtener(request.registroId(), negocioId);
        transaccionPort.ejecutar(() -> registroRepository.deleteById(registro.getId()));
    }
}
