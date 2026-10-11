package com.ensambla.backend.domain.service.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
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

    private final ArchivoCuadernoPort archivoCuadernoPort;

    /**
     * Valida las fotos, las guarda en la carpeta privada del negocio y guarda el lote pendiente. Si algo falla a mitad
     * de camino borra lo que ya se había guardado, así no quedan archivos sin lote.
     */
    @Transactional
    public LoteSubida subir(UUID negocioId, List<byte[]> contenidos, OffsetDateTime ahora) {
        LoteSubida lote = LoteSubida.nuevo(negocioId, contenidos, ahora);
        List<String> subidas = new ArrayList<>();
        try {
            for (FotoCuaderno foto : lote.getFotos()) {
                archivoCuadernoPort.guardar(negocioId, foto.getClaveAlmacenamiento(), foto.getContenido());
                subidas.add(foto.getClaveAlmacenamiento());
            }
            return loteSubidaRepository.saveConFotos(lote);
        } catch (RuntimeException e) {
            subidas.forEach(clave -> archivoCuadernoPort.eliminar(negocioId, clave));
            throw e;
        }
    }

    /**
     * Marca el lote como confirmado y lo vincula al bloque donde quedaron los datos.
     *
     * @throws BusinessRuleException si otra confirmación se adelantó o el lote ya no estaba listo
     */
    @Transactional
    public void confirmar(LoteSubida lote, UUID bloqueDestinoId, OffsetDateTime ahora) {
        if (!loteSubidaRepository.confirmar(lote.getId(), lote.getNegocioId(), bloqueDestinoId, ahora)) {
            throw new BusinessRuleException("Este cuaderno ya se guardó");
        }
    }

    /** El lote del negocio con sus fotos; un lote ajeno responde como inexistente. */
    public LoteSubida obtener(UUID negocioId, UUID loteId) {
        return loteSubidaRepository.findByIdAndNegocioId(loteId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }
}
