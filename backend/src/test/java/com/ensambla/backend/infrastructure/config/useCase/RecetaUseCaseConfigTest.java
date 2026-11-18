package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerRecetaRequest;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecetaUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    private final RecetaUseCaseConfig config = new RecetaUseCaseConfig();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final RecetaService recetaService = mock(RecetaService.class);
    private final RecetaRepository recetaRepository = mock(RecetaRepository.class);
    private RecetaItem receta;

    @BeforeEach
    void dadoUnProductoSinCostos() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroRepository.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Registro.builder().id(PRODUCTO_ID).build());
        when(recetaService.costear(any(), any())).thenReturn(RecetaCosteada.vacia());
    }

    @Test
    @DisplayName("El wiring de obtener conecta los servicios")
    void elWiringDeObtenerConectaLosServicios() {
        dadaUnaRecetaVaciaGuardada();

        cuandoSeObtieneLaReceta();

        entoncesLaRecetaNoTieneLineasNiCosto();
    }

    @Test
    @DisplayName("El wiring de guardar conecta los servicios y el reloj")
    void elWiringDeGuardarConectaLosServiciosYElReloj() {
        cuandoSeGuardaUnaRecetaVacia();

        entoncesLaRecetaNoTieneLineas();
    }

    private void dadaUnaRecetaVaciaGuardada() {
        when(recetaRepository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(Receta.builder().lineas(List.of()).build()));
    }

    private void cuandoSeObtieneLaReceta() {
        receta = config.obtenerReceta(negocioRepository, mock(ControlPlanService.class), Clock.systemUTC(),
                registroRepository, recetaService, recetaRepository).execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID));
    }

    private void cuandoSeGuardaUnaRecetaVacia() {
        receta = config.guardarReceta(negocioRepository, mock(ControlPlanService.class), recetaService, recetaRepository,
                        mock(CampoRepository.class), registroRepository, new TransaccionDirecta(), Clock.systemUTC())
                .execute(new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, null, List.of()));
    }

    private void entoncesLaRecetaNoTieneLineasNiCosto() {
        entoncesLaRecetaNoTieneLineas();
        assertThat(receta.costo()).isEqualByComparingTo("0");
    }

    private void entoncesLaRecetaNoTieneLineas() {
        assertThat(receta.lineas()).isEmpty();
    }
}