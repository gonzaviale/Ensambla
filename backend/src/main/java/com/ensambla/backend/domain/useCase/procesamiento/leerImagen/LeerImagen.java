package com.ensambla.backend.domain.useCase.procesamiento.leerImagen;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen;
import com.ensambla.backend.domain.model.procesamiento.LecturaImagen;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.procesamiento.PropuestaLecturaService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Lee una foto contra un bloque y devuelve los registros y las columnas nuevas para revisar. Es la misma lectura que
 * el lote de fotos (el bloque elegido es el destino y se cruzan columnas por nombre y sinónimos), pero sin guardar
 * nada: la respuesta es inmediata y la persona decide después qué incorporar.
 */
@RequiredArgsConstructor
public class LeerImagen {

    private final LectorCuadernoPort lectorCuadernoPort;

    private final PropuestaLecturaService propuestaLecturaService;

    /** Debajo de este valor de confianza un dato se marca como dudoso. */
    private final double umbralConfianza;

    private final NegocioService negocioService;

    private final ControlPlanService controlPlanService;

    private final UsoPlanRepository usoPlanRepository;

    private final PoliticaProcesamiento politicaProcesamiento;

    private final Clock clock;

    public LeerImagenItem execute(LeerImagenRequest request) {
        ImagenCuaderno imagen = request.aImagenCuaderno();
        BloqueDeImagen bloque = request.aBloqueDeImagen();
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        registrarConsumo(negocioId, OffsetDateTime.now(clock));
        LecturaImagen lectura = leer(imagen, bloque, request.destinoExistente());
        return LeerImagenItem.de(lectura, umbralConfianza);
    }

    /** Registra el intento antes de salir al proveedor; un fallo o timeout también puede tener costo. */
    private void registrarConsumo(UUID negocioId, OffsetDateTime ahora) {
        LocalDate dia = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        if (!controlPlanService.reservarPaginasIa(negocioId, 1, ahora, () -> usoPlanRepository
                .registrarPaginaInmediata(negocioId, dia, politicaProcesamiento.limiteDiario(), ahora))) {
            throw new RestriccionPlanException("LIMITE_IA_DIARIO", PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        }
    }

    private LecturaImagen leer(ImagenCuaderno imagen, BloqueDeImagen bloque, boolean destinoExistente) {
        LecturaCuaderno lectura = lectorCuadernoPort.leer(
                List.of(imagen.comoPagina()), List.of(bloque.paraElLector()), bloque.id());
        PropuestaLectura propuesta = propuestaLecturaService.proponer(
                lectura, List.of(bloque.paraLaPropuesta()), bloque.id());
        return LecturaImagen.desde(propuesta, bloque, destinoExistente);
    }
}
