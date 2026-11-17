package com.ensambla.backend.domain.useCase.bloque.crearBloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.proyecto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearBloqueTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final CrearBloque crearBloque = new BloqueUseCaseConfig().crearBloque(
            negocioRepository,
            proyectoRepository,
            bloqueRepository,
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    private BloqueItem respuesta;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Crea el bloque en el proyecto del negocio de la sesión con la hora del reloj")
    void creaElBloqueEnElProyectoDelNegocioDeLaSesion() {
        dadoUnProyectoDondeSeGuardaUnBloqueLlamadoClientes();

        cuandoSeCreaElBloque("Clientes");

        entoncesSeResponde("Clientes");
    }

    @Test
    @DisplayName("Guarda el bloque que arma el proyecto al final de su lista")
    void guardaElBloqueQueArmaElProyectoAlFinalDeSuLista() {
        dadoUnProyectoQueGuardaLoQueRecibe();

        cuandoSeCreaElBloque("Clientes");

        entoncesSeGuardaElBloqueNuevoAlFinalDelProyecto();
    }

    @Test
    @DisplayName("Con un nombre repetido en el proyecto no guarda nada")
    void conUnNombreRepetidoEnElProyectoNoGuardaNada() {
        dadoUnProyectoConUnBloqueLlamadoProductos();

        cuandoSeIntentaCrearElBloque("Productos");

        entoncesSeRechazaPorNombreRepetido();
        entoncesNoSeGuardaNingunBloque();
    }

    @Test
    @DisplayName("Si el proyecto es ajeno no crea ningún bloque")
    void siElProyectoEsAjenoNoCreaNingunBloque() {
        dadoUnProyectoAjeno();

        cuandoSeIntentaCrearElBloque("Clientes");

        entoncesSeRechazaPorqueElProyectoNoExiste();
        entoncesNoSeGuardaNingunBloque();
    }

    // --- dado ---
    private void dadoUnProyectoDondeSeGuardaUnBloqueLlamadoClientes() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto());
        when(bloqueRepository.save(any(Bloque.class))).thenReturn(unBloqueGuardadoLlamadoClientes());
    }

    private void dadoUnProyectoQueGuardaLoQueRecibe() {
        dadoUnProyectoConUnBloqueLlamadoProductos();
        when(bloqueRepository.save(any(Bloque.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnProyectoConUnBloqueLlamadoProductos() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto());
    }

    private void dadoUnProyectoAjeno() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    private Bloque unBloqueGuardadoLlamadoClientes() {
        return Bloque.builder()
                .id(BLOQUE_ID)
                .proyectoId(PROYECTO_ID)
                .nombre("Clientes")
                .posicion((short) 2)
                .build();
    }

    // --- cuando ---
    private void cuandoSeCreaElBloque(String nombre) {
        respuesta = crearBloque.execute(new CrearBloqueRequest(USUARIO_ID, PROYECTO_ID, nombre));
    }

    private void cuandoSeIntentaCrearElBloque(String nombre) {
        rechazo = catchThrowable(() -> cuandoSeCreaElBloque(nombre));
    }

    // --- entonces ---
    private void entoncesSeResponde(String nombre) {
        assertThat(respuesta.id()).isEqualTo(BLOQUE_ID);
        assertThat(respuesta.nombre()).isEqualTo(nombre);
    }

    private void entoncesSeGuardaElBloqueNuevoAlFinalDelProyecto() {
        ArgumentCaptor<Bloque> guardado = ArgumentCaptor.forClass(Bloque.class);
        verify(bloqueRepository).save(guardado.capture());
        assertThat(guardado.getValue().getNombre()).isEqualTo("Clientes");
        assertThat(guardado.getValue().getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(guardado.getValue().getPosicion()).isEqualTo((short) 2);
        assertThat(guardado.getValue().getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeRechazaPorNombreRepetido() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesSeRechazaPorqueElProyectoNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardaNingunBloque() {
        verifyNoInteractions(bloqueRepository);
    }
}
