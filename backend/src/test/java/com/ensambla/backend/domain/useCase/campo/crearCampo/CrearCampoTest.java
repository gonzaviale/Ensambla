package com.ensambla.backend.domain.useCase.campo.crearCampo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearCampoTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");
    private static final List<String> TALLES = List.of("Chico", "Grande");
    private static final CrearCampoRequest REQUEST =
            new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES, null, null);
    private static final ConfiguracionCampo CONFIGURACION =
            new ConfiguracionCampo("Talle", "OPCION", true, "Chico", TALLES, null, null);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final BloqueService bloqueService = mock(BloqueService.class);
    private final CampoService campoService = mock(CampoService.class);
    private final CampoUseCaseConfig config = new CampoUseCaseConfig();
    private final CrearCampo crearCampo = config.crearCampo(
            negocioService,
            bloqueService,
            campoService,
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Crea el campo en el bloque del negocio de la sesión con la configuración pedida y la hora del reloj")
    void executeShouldCreateCampoInSessionBloque() {
        Bloque bloque = givenBloque();
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque);
        when(campoService.crear(bloque, CONFIGURACION, AHORA)).thenReturn(givenCampoGuardado());

        CampoItem result = crearCampo.execute(REQUEST);

        assertThat(result.id()).isEqualTo(CAMPO_ID);
        assertThat(result.tipo()).isEqualTo("OPCION");
    }

    @Test
    @DisplayName("Si el bloque es ajeno no crea ningún campo")
    void executeShouldNotCreateWhenBloqueIsForeign() {
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));

        assertThatThrownBy(() -> crearCampo.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(campoService);
    }

    @Test
    @DisplayName("Propaga sin convertir el error de configuración del servicio")
    void executeShouldPropagateCampoInvalido() {
        Bloque bloque = givenBloque();
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque);
        when(campoService.crear(bloque, CONFIGURACION, AHORA))
                .thenThrow(new CampoInvalidoException(Map.of("opciones", "Agregá al menos una opción")));

        assertThatThrownBy(() -> crearCampo.execute(REQUEST))
                .isInstanceOf(CampoInvalidoException.class);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private Bloque givenBloque() {
        return Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).build();
    }

    private Campo givenCampoGuardado() {
        return Campo.builder()
                .id(CAMPO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre("Talle")
                .tipo(TipoCampo.OPCION)
                .obligatorio(true)
                .valorInicial("Chico")
                .opciones(TALLES)
                .posicion((short) 0)
                .build();
    }
}
