package com.ensambla.backend.domain.useCase.bloque.eliminarBloque;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/** No se puede borrar una tabla que otra tabla usa en una conexión; la autorreferencia no cuenta. */
@RequiredArgsConstructor
public class EliminarBloque {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarBloqueRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
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
        Bloque otro = bloqueService.obtener(campo.getBloqueId(), bloque.getNegocioId());
        throw new BusinessRuleException("La tabla «" + bloque.getNombre() + "» se usa en la columna «"
                + campo.getNombre() + "» de «" + otro.getNombre() + "»");
    }
}
