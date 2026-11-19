package com.ensambla.backend.domain.useCase.lote.subirLote;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class SubirLote {

    private final NegocioRepository negocioRepository;

    private final LoteSubidaRepository loteSubidaRepository;

    private final ArchivoCuadernoPort archivoCuadernoPort;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final BloqueRepository bloqueRepository;

    private final Clock clock;

    public LoteItem execute(SubirLoteRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        validarDestino(negocioId, request.bloqueDestinoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return LoteItem.de(subirEnTransaccion(negocioId, request, ahora));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    /** Un bloque de otro negocio responde como inexistente. */
    private void validarDestino(NegocioId negocioId, BloqueId bloqueDestinoId) {
        if (bloqueDestinoId != null) {
            bloqueRepository.obtener(bloqueDestinoId, negocioId);
        }
    }

    private LoteSubida subirEnTransaccion(NegocioId negocioId, SubirLoteRequest request, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(
                () -> subir(negocioId, request.fotos(), request.bloqueDestinoId(), ahora));
    }

    private LoteSubida subir(NegocioId negocioId, List<byte[]> contenidos, BloqueId bloqueDestinoId,
            OffsetDateTime ahora) {
        LoteSubida lote = LoteSubida.nuevo(negocioId, contenidos, bloqueDestinoId, ahora);
        validarPaginasDelPlan(negocioId, lote, ahora);
        return guardarLoteConSusArchivos(negocioId, lote);
    }

    private void validarPaginasDelPlan(NegocioId negocioId, LoteSubida lote, OffsetDateTime ahora) {
        controlPlanService.validarPaginasIa(negocioId, lote.getFotos().size(), ahora);
    }

    /** Si algo falla a mitad de camino borra lo que ya se había guardado, así no quedan archivos sin lote. */
    private LoteSubida guardarLoteConSusArchivos(NegocioId negocioId, LoteSubida lote) {
        List<String> claves = new ArrayList<>();
        try {
            guardarArchivos(negocioId, lote, claves);
            return guardarLote(lote);
        } catch (RuntimeException e) {
            eliminarArchivos(negocioId, claves);
            throw e;
        }
    }

    private void guardarArchivos(NegocioId negocioId, LoteSubida lote, List<String> claves) {
        for (FotoCuaderno foto : lote.getFotos()) {
            guardarArchivo(negocioId, foto);
            claves.add(foto.getClaveAlmacenamiento());
        }
    }

    private void guardarArchivo(NegocioId negocioId, FotoCuaderno foto) {
        archivoCuadernoPort.guardar(negocioId, foto.getClaveAlmacenamiento(), foto.getContenido());
    }

    private LoteSubida guardarLote(LoteSubida lote) {
        return loteSubidaRepository.saveConFotos(lote);
    }

    private void eliminarArchivos(NegocioId negocioId, List<String> claves) {
        claves.forEach(clave -> archivoCuadernoPort.eliminar(negocioId, clave));
    }
}
