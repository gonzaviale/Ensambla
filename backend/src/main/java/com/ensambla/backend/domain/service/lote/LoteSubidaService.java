package com.ensambla.backend.domain.service.lote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.port.AlmacenamientoArchivosPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoteSubidaService {

    private final LoteSubidaRepository loteSubidaRepository;

    private final AlmacenamientoArchivosPort almacenamientoArchivosPort;

    /**
     * Valida las fotos, las sube al bucket y guarda el lote pendiente. Si algo falla a mitad de camino borra del bucket
     * lo que ya había subido, así no quedan archivos sin lote.
     */
    @Transactional
    public LoteSubida subir(UUID negocioId, List<byte[]> contenidos, OffsetDateTime ahora) {
        LoteSubida lote = LoteSubida.nuevo(negocioId, contenidos, ahora);
        List<String> subidas = new ArrayList<>();
        try {
            for (FotoCuaderno foto : lote.getFotos()) {
                almacenamientoArchivosPort.guardar(
                        foto.getClaveAlmacenamiento(), foto.getContenido(), foto.getTipoContenido());
                subidas.add(foto.getClaveAlmacenamiento());
            }
            return loteSubidaRepository.saveConFotos(lote);
        } catch (RuntimeException e) {
            subidas.forEach(almacenamientoArchivosPort::eliminar);
            throw e;
        }
    }

    /** El lote del negocio con una URL firmada de lectura por foto; un lote ajeno responde como inexistente. */
    public LoteSubida obtenerConUrlsDeLectura(UUID negocioId, UUID loteId) {
        LoteSubida lote = loteSubidaRepository.findByIdAndNegocioId(loteId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
        lote.getFotos().forEach(foto -> foto.setUrlLectura(
                almacenamientoArchivosPort.generarUrlLectura(foto.getClaveAlmacenamiento())));
        return lote;
    }
}
