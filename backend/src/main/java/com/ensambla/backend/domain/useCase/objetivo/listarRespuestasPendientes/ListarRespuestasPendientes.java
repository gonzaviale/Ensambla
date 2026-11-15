package com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Los mini-objetivos manuales en curso del negocio que todavía no se respondieron, de todos sus objetivos. Sami
 * pregunta primero por el más viejo.
 */
@RequiredArgsConstructor
public class ListarRespuestasPendientes {

    private final NegocioRepository negocioRepository;

    private final ObjetivoRepository objetivoRepository;

    private final MiniObjetivoRepository miniObjetivoRepository;

    public ListarRespuestasPendientesResponse execute(ListarRespuestasPendientesRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return ListarRespuestasPendientesResponse.de(
                obtenerObjetivos(negocioId), obtenerMinisPendientes(negocioId));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private List<Objetivo> obtenerObjetivos(NegocioId negocioId) {
        return objetivoRepository.findAllByNegocioId(negocioId);
    }

    private List<MiniObjetivo> obtenerMinisPendientes(NegocioId negocioId) {
        return miniObjetivoRepository.findAllPendientesDeRespuestaByNegocioId(negocioId);
    }
}
