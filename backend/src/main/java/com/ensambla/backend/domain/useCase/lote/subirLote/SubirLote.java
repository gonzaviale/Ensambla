package com.ensambla.backend.domain.useCase.lote.subirLote;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class SubirLote {

    private final NegocioService negocioService;

    private final LoteSubidaRepository loteSubidaRepository;

    private final ArchivoCuadernoPort archivoCuadernoPort;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final BloqueService bloqueService;

    private final Clock clock;

    public LoteItem execute(SubirLoteRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        validarDestino(negocioId, request.bloqueDestinoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        LoteSubida lote = transaccionPort.ejecutar(
                () -> subir(negocioId, request.fotos(), request.bloqueDestinoId(), ahora));
        return LoteItem.de(lote);
    }

    /** Un bloque de otro negocio responde como inexistente. */
    private void validarDestino(NegocioId negocioId, BloqueId bloqueDestinoId) {
        if (bloqueDestinoId != null) {
            bloqueService.obtener(bloqueDestinoId, negocioId);
        }
    }

    /**
     * Valida las fotos, las guarda en la carpeta privada del negocio y guarda el lote pendiente. Si algo falla a mitad
     * de camino borra lo que ya se había guardado, así no quedan archivos sin lote.
     */
    private LoteSubida subir(NegocioId negocioId, List<byte[]> contenidos, BloqueId bloqueDestinoId,
            OffsetDateTime ahora) {
        LoteSubida lote = LoteSubida.nuevo(negocioId, contenidos, bloqueDestinoId, ahora);
        controlPlanService.validarPaginasIa(negocioId, lote.getFotos().size(), ahora);
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
}
