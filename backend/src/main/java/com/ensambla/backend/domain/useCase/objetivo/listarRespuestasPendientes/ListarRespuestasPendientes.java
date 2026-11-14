package com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import lombok.RequiredArgsConstructor;

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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return ListarRespuestasPendientesResponse.de(
                objetivoRepository.findAllByNegocioId(negocioId),
                miniObjetivoRepository.findAllPendientesDeRespuestaByNegocioId(negocioId));
    }
}
