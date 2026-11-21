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
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.procesamiento.leerImagen.LeerImagenItem.CampoItem;
import com.ensambla.backend.support.ImagenTestData;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LeerImagenTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("b9000000-0000-4000-8000-000000000002");
    private static final double UMBRAL_CONFIANZA = 0.7;
    /** 23:30 en Argentina ya es el 1 de octubre en UTC: el límite diario se cuenta por día UTC. */
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-30T23:30:00-03:00");

    private final LectorCuadernoPort lectorCuadernoPort = mock(LectorCuadernoPort.class);
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final LeerImagen leerImagen = new LeerImagen(lectorCuadernoPort, UMBRAL_CONFIANZA, negocioRepository,
            controlPlanService, ProcesamientoTestData.politica(),
            Clock.fixed(AHORA.toInstant(), AHORA.getOffset()));

    private final LeerImagenRequest request = new LeerImagenRequest(USUARIO_ID, ImagenTestData.jpeg(), "Ventas",
            List.of(new LeerImagenRequest.CampoItem("importe", "Monto", "NUMERO", null)), true);

    private LeerImagenItem leido;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve lo leído con las claves del bloque y la columna que falta como nueva")
    void devuelveLoLeidoConLasClavesDelBloqueYLaColumnaQueFaltaComoNueva() {
        dadoQueLaIaLeeMontoYTelefono();

        cuandoSeLeeLaImagen();

        entoncesSeDevuelveLoLeidoPorClaveDelBloqueConLaColumnaNuevaDeTelefono();
    }

    @Test
    @DisplayName("La IA ve sólo el bloque elegido: una página y el bloque como destino")
    void laIaVeSoloElBloqueElegidoUnaPaginaYElBloqueComoDestino() {
        dadoQueLaIaLeeSoloElMonto();

        cuandoSeLeeLaImagen();

        entoncesLaIaRecibioUnaPaginaYElBloqueElegidoComoDestino();
    }

    @Test
    @DisplayName("Valida el mes y registra una página en el día UTC antes de invocar la IA")
    void validaElMesYRegistraUnaPaginaEnElDiaUtcAntesDeInvocarLaIa() {
        dadoQueLaIaLeeSoloElMonto();

        cuandoSeLeeLaImagen();

        entoncesSeRegistroLaPaginaAntesDeInvocarLaIa();
    }

    @Test
    @DisplayName("Con el límite diario agotado responde una restricción clara y no llama a la IA")
    void conElLimiteDiarioAgotadoRespondeUnaRestriccionClaraYNoLlamaALaIa() {
        dadoQueSeAgotoElLimiteDiario();

        cuandoSeIntentaLeerLaImagen(request);

        entoncesSeRechazaPorLimiteDiarioSinLlamarALaIa();
    }

    @Test
    @DisplayName("Rechaza la foto cuando ninguna columna leída es del bloque")
    void rechazaLaFotoCuandoNingunaColumnaLeidaEsDelBloque() {
        dadoQueLaIaLeeUnaColumnaQueNoEsDelBloque();

        cuandoSeIntentaLeerLaImagen(request);

        entoncesSeRechazaPorFotoIncompatible();
    }

    @Test
    @DisplayName("Propaga el error de la IA")
    void propagaElErrorDeLaIa() {
        dadoQueLaIaFalla();

        cuandoSeIntentaLeerLaImagen(request);

        entoncesSePropagaElErrorDeLaIa();
    }

    @Test
    @DisplayName("Con una imagen inválida no cobra ni llega a leer")
    void conUnaImagenInvalidaNoCobraNiLlegaALeer() {
        cuandoSeIntentaLeerLaImagen(unPedidoConImagenVacia());

        entoncesSeRechazaPorImagenVaciaSinCobrarNiLeer();
    }

    @Test
    @DisplayName("Con un bloque sin datos no cobra ni llega a leer")
    void conUnBloqueSinDatosNoCobraNiLlegaALeer() {
        cuandoSeIntentaLeerLaImagen(unPedidoSinCampos());

        entoncesSeRechazaSinCobrarNiLeer();
    }

    // --- dado ---
    private void dadoQueLaIaLeeMontoYTelefono() {
        dadoQueLaIaLee(List.of(unaColumna("monto", "Monto", TipoCampo.NUMERO),
                        unaColumna("telefono", "Teléfono", TipoCampo.TEXTO)),
                new FilaLeida(1, 1, Map.of("monto", new DatoLeido("1500", 0.95),
                        "telefono", new DatoLeido("1155", 0.5))));
    }

    private void dadoQueLaIaLeeSoloElMonto() {
        dadoQueLaIaLee(List.of(unaColumna("monto", "Monto", TipoCampo.NUMERO)), new FilaLeida(1, 1, Map.of()));
    }

    private void dadoQueLaIaLeeUnaColumnaQueNoEsDelBloque() {
        dadoQueLaIaLee(List.of(unaColumna("color", "Color", TipoCampo.TEXTO)),
                new FilaLeida(1, 1, Map.of("color", new DatoLeido("Rojo", 0.9))));
    }

    private void dadoQueLaIaLee(List<CampoPropuesto> campos, FilaLeida... filas) {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(new LecturaCuaderno(
                TipoRegistroCuaderno.OTRO, new BloquePropuesto("Hoja", null, campos), List.of(filas), List.of(),
                new ConsumoIa("simulado", "demo", "v3", 1, 0, 0, 1)));
    }

    private void dadoQueSeAgotoElLimiteDiario() {
        doThrow(new RestriccionPlanException("LIMITE_IA_DIARIO", PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO))
                .when(controlPlanService).reservarPaginaIaInmediata(NEGOCIO_ID, 100, AHORA);
    }

    private void dadoQueLaIaFalla() {
        when(lectorCuadernoPort.leer(any(), any(), any())).thenThrow(new LecturaCuadernoException("Falla de la IA"));
    }

    private CampoPropuesto unaColumna(String clave, String nombre, TipoCampo tipo) {
        return new CampoPropuesto(clave, nombre, tipo, List.of());
    }

    private LeerImagenRequest unPedidoConImagenVacia() {
        return new LeerImagenRequest(USUARIO_ID, new byte[0], "Ventas", request.campos(), true);
    }

    private LeerImagenRequest unPedidoSinCampos() {
        return new LeerImagenRequest(USUARIO_ID, request.contenido(), "Ventas", List.of(), true);
    }

    // --- cuando ---
    private void cuandoSeLeeLaImagen() {
        leido = leerImagen.execute(request);
    }

    private void cuandoSeIntentaLeerLaImagen(LeerImagenRequest pedido) {
        rechazo = catchThrowable(() -> leerImagen.execute(pedido));
    }

    // --- entonces ---
    private void entoncesSeDevuelveLoLeidoPorClaveDelBloqueConLaColumnaNuevaDeTelefono() {
        assertThat(leido.camposNuevos()).extracting(CampoItem::clave, CampoItem::nombre)
                .containsExactly(tuple("telefono", "Teléfono"));
        assertThat(leido.filas().getFirst().keySet()).containsExactlyInAnyOrder("importe", "telefono");
        assertThat(leido.filas().getFirst().get("importe").valor()).isEqualTo("1500");
        assertThat(leido.filas().getFirst().get("importe").dudoso()).isFalse();
        assertThat(leido.filas().getFirst().get("telefono").dudoso()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private void entoncesLaIaRecibioUnaPaginaYElBloqueElegidoComoDestino() {
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

    private void entoncesSeRegistroLaPaginaAntesDeInvocarLaIa() {
        InOrder orden = inOrder(controlPlanService, lectorCuadernoPort);
        orden.verify(controlPlanService).reservarPaginaIaInmediata(NEGOCIO_ID, 100, AHORA);
        orden.verify(lectorCuadernoPort).leer(any(), any(), any());
    }

    private void entoncesSeRechazaPorLimiteDiarioSinLlamarALaIa() {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class)
                .hasMessage("Se alcanzó el límite diario de análisis de foto. Probá mañana.");
        verifyNoInteractions(lectorCuadernoPort);
    }

    private void entoncesSeRechazaPorFotoIncompatible() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining("compatible");
    }

    private void entoncesSePropagaElErrorDeLaIa() {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class).hasMessage("Falla de la IA");
    }

    private void entoncesSeRechazaPorImagenVaciaSinCobrarNiLeer() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage("La imagen está vacía");
        verifyNoInteractions(lectorCuadernoPort, controlPlanService);
    }

    private void entoncesSeRechazaSinCobrarNiLeer() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(lectorCuadernoPort, controlPlanService);
    }
}
