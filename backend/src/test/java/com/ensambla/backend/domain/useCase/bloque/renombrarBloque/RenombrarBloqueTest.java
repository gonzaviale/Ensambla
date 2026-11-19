package com.ensambla.backend.domain.useCase.bloque.renombrarBloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.proyecto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RenombrarBloqueTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class, CALLS_REAL_METHODS);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final RenombrarBloque renombrarBloque = new BloqueUseCaseConfig().renombrarBloque(
            negocioRepository,
            proyectoRepository,
            bloqueRepository,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    private final Proyecto proyecto = proyecto();

    private Bloque bloque;
    private BloqueItem respuesta;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Renombra validando contra los bloques del proyecto dueño y guarda con la hora del reloj")
    void renombraValidandoContraLosBloquesDelProyectoDuenioYGuardaConLaHoraDelReloj() {
        dadoElBloqueDelProyectoEnLaPosicion(0);
        dadoQueElRepositorioGuardaLoQueRecibe();

        cuandoSeRenombraElBloque(" Mercadería ");

        entoncesElBloqueSeLlamaMercaderiaYSeActualizoAhora();
        entoncesSeGuardaElBloque();
    }

    @Test
    @DisplayName("Renombrar con el nombre de otro bloque no modifica ni guarda el bloque")
    void renombrarConElNombreDeOtroBloqueNoModificaNiGuardaElBloque() {
        dadoElBloqueDelProyectoEnLaPosicion(1);

        cuandoSeIntentaRenombrarElBloque("Productos");

        entoncesSeRechazaPorNombreRepetido();
        entoncesElBloqueConservaSuNombre("Vacío");
        entoncesNoSeGuardaNingunBloque();
    }

    @Test
    @DisplayName("Un bloque ajeno propaga el 404 sin consultar el proyecto ni renombrar")
    void unBloqueAjenoPropagaElNoEncontradoSinConsultarElProyectoNiRenombrar() {
        dadoUnBloqueAjeno();

        cuandoSeIntentaRenombrarElBloque("Compradores");

        entoncesSeRechazaPorqueElBloqueNoExiste();
        entoncesNoSeConsultaElProyecto();
        entoncesNoSeGuardaNingunBloque();
    }

    // --- dado ---
    private void dadoElBloqueDelProyectoEnLaPosicion(int indice) {
        bloque = proyecto.getBloques().get(indice);
        when(bloqueRepository.findByIdAndNegocioId(bloque.getId(), NEGOCIO_ID)).thenReturn(Optional.of(bloque));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
    }

    private void dadoQueElRepositorioGuardaLoQueRecibe() {
        when(bloqueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnBloqueAjeno() {
        bloque = proyecto.getBloques().getFirst();
        when(bloqueRepository.findByIdAndNegocioId(bloque.getId(), NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeRenombraElBloque(String nombre) {
        respuesta = renombrarBloque.execute(new RenombrarBloqueRequest(USUARIO_ID, bloque.getId(), nombre));
    }

    private void cuandoSeIntentaRenombrarElBloque(String nombre) {
        rechazo = catchThrowable(() -> cuandoSeRenombraElBloque(nombre));
    }

    // --- entonces ---
    private void entoncesElBloqueSeLlamaMercaderiaYSeActualizoAhora() {
        assertThat(respuesta.id()).isEqualTo(bloque.getId());
        assertThat(respuesta.nombre()).isEqualTo("Mercadería");
        assertThat(respuesta.actualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElBloqueConservaSuNombre(String nombre) {
        assertThat(bloque.getNombre()).isEqualTo(nombre);
    }

    private void entoncesSeRechazaPorNombreRepetido() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesSeRechazaPorqueElBloqueNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesSeGuardaElBloque() {
        verify(bloqueRepository).save(bloque);
    }

    private void entoncesNoSeGuardaNingunBloque() {
        verify(bloqueRepository, never()).save(any());
    }

    private void entoncesNoSeConsultaElProyecto() {
        verifyNoInteractions(proyectoRepository);
    }
}
