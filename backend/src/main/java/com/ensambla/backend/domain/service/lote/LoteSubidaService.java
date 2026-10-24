package com.ensambla.backend.domain.service.lote;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoteSubidaService {

    private final LoteSubidaRepository loteSubidaRepository;

    private final ArchivoCuadernoPort archivoCuadernoPort;

    /** El lote del negocio con sus fotos; un lote ajeno responde como inexistente. */
    public LoteSubida obtener(NegocioId negocioId, LoteId loteId) {
        return loteSubidaRepository.findByIdAndNegocioId(loteId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }

    /** Una foto registrada cuyo archivo ya no está o no se puede abrir se muestra como no disponible, no como error. */
    public ArchivoFoto leer(NegocioId negocioId, FotoCuaderno foto) {
        try {
            return new ArchivoFoto(
                    archivoCuadernoPort.leer(negocioId, foto.getClaveAlmacenamiento()), foto.getTipoContenido());
        } catch (LecturaCuadernoException e) {
            throw new ResourceNotFoundException("La foto ya no está disponible");
        }
    }
}
