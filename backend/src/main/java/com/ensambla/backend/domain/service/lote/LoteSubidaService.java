package com.ensambla.backend.domain.service.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class LoteSubidaService {

    private final LoteSubidaRepository loteSubidaRepository;

    private final ArchivoCuadernoPort archivoCuadernoPort;

    private final TransaccionPort transaccionPort;

    /**
     * Marca el lote como confirmado y lo vincula al bloque donde quedaron los datos.
     *
     * @throws BusinessRuleException si otra confirmación se adelantó o el lote ya no estaba listo
     */
    public void confirmar(LoteSubida lote, UUID bloqueDestinoId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            if (!loteSubidaRepository.confirmar(lote.getId(), lote.getNegocioId(), bloqueDestinoId, ahora)) {
                throw new BusinessRuleException("Este cuaderno ya se guardó");
            }
        });
    }

    /** El lote del negocio con sus fotos; un lote ajeno responde como inexistente. */
    public LoteSubida obtener(UUID negocioId, UUID loteId) {
        return loteSubidaRepository.findByIdAndNegocioId(loteId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }

    /** Una foto registrada cuyo archivo ya no está o no se puede abrir se muestra como no disponible, no como error. */
    public ArchivoFoto leer(UUID negocioId, FotoCuaderno foto) {
        try {
            return new ArchivoFoto(
                    archivoCuadernoPort.leer(negocioId, foto.getClaveAlmacenamiento()), foto.getTipoContenido());
        } catch (LecturaCuadernoException e) {
            throw new ResourceNotFoundException("La foto ya no está disponible");
        }
    }
}
