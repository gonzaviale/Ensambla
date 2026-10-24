package com.ensambla.backend.domain.useCase.procesamiento.leerImagen;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.procesamiento.PropuestaLecturaService;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenItem.CampoItem;
import com.ensambla.backend.support.ImagenTestData;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LeerImagenTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("b9000000-0000-4000-8000-000000000002");
    private static final double UMBRAL_CONFIANZA = 0.7;
    /** 23:30 en Argentina ya es el 1 de octubre en UTC: el límite diario se cuenta por día UTC. */
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-30T23:30:00-03:00");
    private static final LocalDate DIA_UTC = LocalDate.parse("2026-10-01");

    private final LectorCuadernoPort lectorCuadernoPort = mock(LectorCuadernoPort.class);
    private final NegocioService negocioService = mock(NegocioService.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final LeerImagen leerImagen = new LeerImagen(lectorCuadernoPort, new PropuestaLecturaService(),
            UMBRAL_CONFIANZA, negocioService, controlPlanService, usoPlanRepository, ProcesamientoTestData.politica(),
            Clock.fixed(AHORA.toInstant(), AHORA.getOffset()));

    private final LeerImagenRequest request = new LeerImagenRequest(USUARIO_ID, ImagenTestData.jpeg(), "Ventas",
            List.of(new LeerImagenRequest.CampoItem("importe", "Monto", "NUMERO", null)), true);

    @BeforeEach
    void givenNegocioConCupo() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(controlPlanService.reservarPaginasIa(eq(NEGOCIO_ID), eq(1), eq(AHORA), any()))
                .thenAnswer(invocacion -> invocacion.<BooleanSupplier>getArgument(3).getAsBoolean());
        when(usoPlanRepository.registrarPaginaInmediata(NEGOCIO_ID, DIA_UTC, 100, AHORA)).thenReturn(true);
    }

    @Test
    @DisplayName("Devuelve lo leído con las claves del bloque y la columna que falta como nueva")
    void executeShouldReturnTheReadingWithTheBlockKeys() {
        givenLectura(List.of(columna("monto", "Monto"), columna("telefono", "Teléfono")),
                new FilaLeida(1, 1, Map.of("monto", new DatoLeido("1500", 0.95),
                        "telefono", new DatoLeido("1155", 0.5))));

        LeerImagenItem item = leerImagen.execute(request);

        assertThat(item.camposNuevos()).extracting(CampoItem::clave, CampoItem::nombre)
                .containsExactly(tuple("telefono", "Teléfono"));
        assertThat(item.filas().getFirst().keySet()).containsExactlyInAnyOrder("importe", "telefono");
        assertThat(item.filas().getFirst().get("importe").valor()).isEqualTo("1500");
        assertThat(item.filas().getFirst().get("importe").dudoso()).isFalse();
        assertThat(item.filas().getFirst().get("telefono").dudoso()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("La IA ve sólo el bloque elegido: una página y el bloque como destino")
    void executeShouldAskTheReaderForOnlyTheChosenBlock() {
        givenLectura(List.of(columna("monto", "Monto")), new FilaLeida(1, 1, Map.of()));

        leerImagen.execute(request);

        ArgumentCaptor<List<PaginaCuaderno>> paginas = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<BloqueConocido>> bloques = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<BloqueId> destino = ArgumentCaptor.forClass(BloqueId.class);
        verify(lectorCuadernoPort).leer(paginas.capture(), bloques.capture(), destino.capture());
        assertThat(paginas.getValue()).singleElement().satisfies(pagina -> {
            assertThat(pagina.contenido()).isEqualTo(request.contenido());
            assertThat(pagina.tipoContenido()).isEqualTo("image/jpeg");
        });
        assertThat(bloques.getValue()).singleElement().satisfies(bloque -> {
            assertThat(bloque.id()).isEqualTo(destino.getValue());
            assertThat(bloque.nombre()).isEqualTo("Ventas");
            assertThat(bloque.campos()).extracting("nombre").containsExactly("Monto");
        });
    }

    @Test
    @DisplayName("Valida el mes y registra una página en el día UTC antes de invocar la IA")
    void executeShouldChargeTheUtcDayBeforeReading() {
        givenLectura(List.of(columna("monto", "Monto")), new FilaLeida(1, 1, Map.of()));

        leerImagen.execute(request);

        InOrder orden = inOrder(controlPlanService, usoPlanRepository, lectorCuadernoPort);
        orden.verify(controlPlanService).reservarPaginasIa(eq(NEGOCIO_ID), eq(1), eq(AHORA), any());
        orden.verify(usoPlanRepository).registrarPaginaInmediata(NEGOCIO_ID, DIA_UTC, 100, AHORA);
        orden.verify(lectorCuadernoPort).leer(any(), any(), any());
    }

    @Test
    @DisplayName("Con el límite diario agotado responde una restricción clara y no llama a la IA")
    void executeShouldRejectDailyLimit() {
        when(usoPlanRepository.registrarPaginaInmediata(NEGOCIO_ID, DIA_UTC, 100, AHORA)).thenReturn(false);

        assertThatThrownBy(() -> leerImagen.execute(request))
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessage("Se alcanzó el límite diario de páginas de IA. Probá mañana.");

        verifyNoInteractions(lectorCuadernoPort);
    }

    @Test
    @DisplayName("Rechaza la foto cuando ninguna columna leída es del bloque")
    void executeShouldRejectAPhotoThatIsNotFromTheBlock() {
        givenLectura(List.of(columna("color", "Color")),
                new FilaLeida(1, 1, Map.of("color", new DatoLeido("Rojo", 0.9))));

        assertThatThrownBy(() -> leerImagen.execute(request))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("compatible");
    }

    @Test
    @DisplayName("Propaga el error de la IA")
    void executeShouldPropagateTheReaderFailure() {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Falla de la IA"));

        assertThatThrownBy(() -> leerImagen.execute(request))
                .isInstanceOf(LecturaCuadernoException.class).hasMessage("Falla de la IA");
    }

    @Test
    @DisplayName("Con una imagen inválida no cobra ni llega a leer")
    void executeShouldNotReadAnInvalidImage() {
        LeerImagenRequest vacia = new LeerImagenRequest(USUARIO_ID, new byte[0], "Ventas", request.campos(), true);

        assertThatThrownBy(() -> leerImagen.execute(vacia))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La imagen está vacía");

        verifyNoInteractions(lectorCuadernoPort, controlPlanService);
    }

    @Test
    @DisplayName("Con un bloque sin datos no cobra ni llega a leer")
    void executeShouldNotReadABlockWithoutFields() {
        LeerImagenRequest sinCampos = new LeerImagenRequest(USUARIO_ID, request.contenido(), "Ventas", List.of(), true);

        assertThatThrownBy(() -> leerImagen.execute(sinCampos)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(lectorCuadernoPort, controlPlanService);
        verify(usoPlanRepository, never()).registrarPaginaInmediata(any(), any(), anyInt(), any());
    }

    // --- arrange ---
    private void givenLectura(List<CampoPropuesto> campos, FilaLeida... filas) {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(new LecturaCuaderno(
                TipoRegistroCuaderno.OTRO, new BloquePropuesto("Hoja", null, campos), List.of(filas), List.of(),
                new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1)));
    }

    private CampoPropuesto columna(String clave, String nombre) {
        return new CampoPropuesto(clave, nombre, nombre.equals("Monto") ? TipoCampo.NUMERO : TipoCampo.TEXTO, List.of());
    }
}
