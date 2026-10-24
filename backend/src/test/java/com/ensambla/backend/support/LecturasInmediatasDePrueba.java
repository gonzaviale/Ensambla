package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.procesamiento.PropuestaLecturaService;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagen;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lee una foto con el caso de uso real en el instante que elige cada test, para medir el consumo inmediato contra
 * PostgreSQL. La IA es un doble que devuelve una lectura compatible con el bloque.
 */
@Component
@RequiredArgsConstructor
public class LecturasInmediatasDePrueba {

    private static final double UMBRAL_CONFIANZA = 0.7;

    private final ControlPlanService controlPlanService;

    private final UsoPlanRepository usoPlanRepository;

    private final PoliticaProcesamiento politicaProcesamiento;

    private final TransaccionPort transaccionPort;

    public void registrar(UUID negocioId, OffsetDateTime ahora) {
        new LeerImagen(lectorCompatible(), new PropuestaLecturaService(), UMBRAL_CONFIANZA, negocioPropio(negocioId),
                controlPlanService, usoPlanRepository, politicaProcesamiento, transaccionPort,
                Clock.fixed(ahora.toInstant(), ahora.getOffset()))
                .execute(new LeerImagenRequest(negocioId, ImagenTestData.jpeg(), "Ventas",
                        List.of(new LeerImagenRequest.CampoItem("importe", "Monto", "NUMERO", null)), true));
    }

    private static LectorCuadernoPort lectorCompatible() {
        LectorCuadernoPort lector = mock(LectorCuadernoPort.class);
        when(lector.leer(any(), any(), any())).thenReturn(new LecturaCuaderno(TipoRegistroCuaderno.OTRO,
                new BloquePropuesto("Ventas", null,
                        List.of(new CampoPropuesto("monto", "Monto", TipoCampo.NUMERO, List.of()))),
                List.of(new FilaLeida(1, 1, Map.of())), List.of(), new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1)));
        return lector;
    }

    private static NegocioService negocioPropio(UUID negocioId) {
        NegocioService negocioService = mock(NegocioService.class);
        when(negocioService.obtenerDelUsuario(negocioId)).thenReturn(negocioId);
        return negocioService;
    }
}
