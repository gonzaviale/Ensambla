package com.ensambla.backend.domain.useCase.bloque.eliminarBloque;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** No se puede borrar una tabla que otra tabla usa en una conexión; la autorreferencia no cuenta. */
@RequiredArgsConstructor
public class EliminarBloque {

    private final NegocioRepository negocioRepository;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarBloqueRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Bloque bloque = bloqueRepository.obtener(request.bloqueId(), negocioId);
        transaccionPort.ejecutar(() -> eliminar(bloque));
    }

    private void eliminar(Bloque bloque) {
        validarSinConexionesHacia(bloque);
        bloqueRepository.deleteByIdAndNegocioId(bloque.getId(), bloque.getNegocioId());
    }

    private void validarSinConexionesHacia(Bloque bloque) {
        List<Campo> conexiones = campoRepository.findRelacionesHacia(bloque.getId(), bloque.getNegocioId());
        if (conexiones.isEmpty()) {
            return;
        }
        Campo campo = conexiones.getFirst();
        Bloque otro = bloqueRepository.obtener(campo.getBloqueId(), bloque.getNegocioId());
        throw new BusinessRuleException("La tabla «" + bloque.getNombre() + "» se usa en la columna «"
                + campo.getNombre() + "» de «" + otro.getNombre() + "»");
    }
}
