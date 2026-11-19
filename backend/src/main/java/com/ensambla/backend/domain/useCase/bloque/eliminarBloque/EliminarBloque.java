package com.ensambla.backend.domain.useCase.bloque.eliminarBloque;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request.bloqueId(), negocioId);
        eliminarEnTransaccion(bloque);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(BloqueId bloqueId, NegocioId negocioId) {
        return bloqueRepository.obtener(bloqueId, negocioId);
    }

    private void eliminarEnTransaccion(Bloque bloque) {
        transaccionPort.ejecutar(() -> eliminar(bloque));
    }

    private void eliminar(Bloque bloque) {
        validarSinConexionesHacia(bloque);
        borrar(bloque);
    }

    private void validarSinConexionesHacia(Bloque bloque) {
        List<Campo> conexiones = buscarConexionesHacia(bloque);
        if (conexiones.isEmpty()) {
            return;
        }
        throw bloqueEnUso(bloque, conexiones.getFirst());
    }

    private List<Campo> buscarConexionesHacia(Bloque bloque) {
        return campoRepository.findRelacionesHacia(bloque.getId(), bloque.getNegocioId());
    }

    private BusinessRuleException bloqueEnUso(Bloque bloque, Campo conexion) {
        Bloque otro = obtenerBloque(conexion.getBloqueId(), bloque.getNegocioId());
        return new BusinessRuleException("La tabla «" + bloque.getNombre() + "» se usa en la columna «"
                + conexion.getNombre() + "» de «" + otro.getNombre() + "»");
    }

    private void borrar(Bloque bloque) {
        bloqueRepository.deleteByIdAndNegocioId(bloque.getId(), bloque.getNegocioId());
    }
}
