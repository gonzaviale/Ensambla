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
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class LoteSubidaService {

    private final LoteSubidaRepository loteSubidaRepository;

    private final ArchivoCuadernoPort archivoCuadernoPort;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    /**
     * Valida las fotos, las guarda en la carpeta privada del negocio y guarda el lote pendiente. Si algo falla a mitad
     * de camino borra lo que ya se había guardado, así no quedan archivos sin lote.
     */
    public LoteSubida subir(UUID negocioId, List<byte[]> contenidos, UUID bloqueDestinoId, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
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
        });
    }

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

    /** La foto de un lote del negocio; si el lote o la foto no son del negocio responde como inexistente. */
    public ArchivoFoto leerFotoDeLote(UUID negocioId, UUID loteId, UUID fotoId) {
        FotoCuaderno foto = obtener(negocioId, loteId).getFotos().stream()
                .filter(candidata -> candidata.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        return leer(negocioId, foto);
    }

    /** Una foto del negocio, por ejemplo la de origen de un registro. */
    public ArchivoFoto leerFoto(UUID negocioId, UUID fotoId) {
        FotoCuaderno foto = loteSubidaRepository.findFotoByIdAndNegocioId(fotoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        return leer(negocioId, foto);
    }

    /** El lote del negocio con sus fotos; un lote ajeno responde como inexistente. */
    public LoteSubida obtener(UUID negocioId, UUID loteId) {
        return loteSubidaRepository.findByIdAndNegocioId(loteId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }

    /** Una foto registrada cuyo archivo ya no está o no se puede abrir se muestra como no disponible, no como error. */
    private ArchivoFoto leer(UUID negocioId, FotoCuaderno foto) {
        try {
            return new ArchivoFoto(
                    archivoCuadernoPort.leer(negocioId, foto.getClaveAlmacenamiento()), foto.getTipoContenido());
        } catch (LecturaCuadernoException e) {
            throw new ResourceNotFoundException("La foto ya no está disponible");
        }
    }
}
