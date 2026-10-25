package com.ensambla.backend.domain.useCase.campo.crearCampo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearCampoTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId RELACIONADO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId OTRO_CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000002");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000002");
    private static final List<String> TALLES = List.of("Chico", "Grande");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class, CALLS_REAL_METHODS);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final CampoUseCaseConfig config = new CampoUseCaseConfig();
    private final CrearCampo crearCampo = config.crearCampo(
            negocioRepository,
            bloqueRepository,
            campoRepository,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Guarda el campo nuevo al final de los del bloque de la sesión, con la hora del reloj")
    void executeShouldSaveCampoAfterTheLastOne() {
        givenBloqueConCampos(givenCampo(OTRO_CAMPO_ID, "Producto", 1));
        givenSaveReturnsArgument();

        CampoItem result = crearCampo.execute(request(" Precio ", "TEXTO", null, null));

        Campo guardado = thenCampoGuardado();
        assertThat(guardado.getNombre()).isEqualTo("Precio");
        assertThat(guardado.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getPosicion()).isEqualTo((short) 2);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.nombre()).isEqualTo("Precio");
    }

    @Test
    @DisplayName("Devuelve el campo con la configuración pedida")
    void executeShouldMapSavedCampo() {
        givenBloqueConCampos();
        givenSaveReturnsArgument();

        CampoItem result = crearCampo.execute(new CrearCampoRequest(
                USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES, null, null));

        assertThat(result.tipo()).isEqualTo("OPCION");
        assertThat(result.opciones()).containsExactlyElementsOf(TALLES);
    }

    @Test
    @DisplayName("Si el bloque es ajeno no crea ningún campo")
    void executeShouldNotCreateWhenBloqueIsForeign() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crearCampo.execute(request("Talle", "TEXTO", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(campoRepository);
    }

    @Test
    @DisplayName("Con un nombre repetido en el bloque no guarda nada")
    void executeShouldNotSaveWhenNameIsTaken() {
        givenBloqueConCampos(givenCampo(OTRO_CAMPO_ID, "Producto", 0));

        assertThatThrownBy(() -> crearCampo.execute(request("producto", "TEXTO", null, null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Con una configuración inválida no guarda nada")
    void executeShouldNotSaveWhenConfiguracionIsInvalid() {
        givenBloqueConCampos();

        assertThatThrownBy(() -> crearCampo.execute(request("Talle", "OPCION", null, null)))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear un campo que no es de conexión no consulta otras tablas")
    void executeShouldNotLookUpBloqueWhenCampoIsNotRelacion() {
        givenBloqueConCampos();
        givenSaveReturnsArgument();

        crearCampo.execute(request("Precio", "TEXTO", null, null));

        verify(bloqueRepository, times(1)).findByIdAndNegocioId(any(), any());
    }

    @Test
    @DisplayName("Guarda una conexión con una tabla del mismo sistema")
    void executeShouldSaveRelacionWhenBloqueIsOfSameProyecto() {
        givenBloqueConCampos();
        givenBloqueRelacionado(PROYECTO_ID);
        givenSaveReturnsArgument();

        crearCampo.execute(request("Cliente", "RELACION", RELACIONADO_ID, null));

        Campo guardado = thenCampoGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(guardado.getBloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    @Test
    @DisplayName("Rechaza una conexión con una tabla inexistente o de otro negocio")
    void executeShouldRejectRelacionWhenBloqueIsMissing() {
        givenBloqueConCampos();
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crearCampo.execute(request("Cliente", "RELACION", RELACIONADO_ID, null)))
                .isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                        .isEqualTo(Map.of("bloqueRelacionadoId", "Elegí una tabla de este sistema")));
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza una conexión con una tabla de otro sistema")
    void executeShouldRejectRelacionWhenBloqueIsOfOtherProyecto() {
        givenBloqueConCampos();
        givenBloqueRelacionado(OTRO_PROYECTO_ID);

        assertThatThrownBy(() -> crearCampo.execute(request("Cliente", "RELACION", RELACIONADO_ID, null)))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guarda una fórmula que usa campos numéricos del bloque")
    void executeShouldSaveFormulaThatUsesNumericFields() {
        givenBloqueConCampos(givenCampo(OTRO_CAMPO_ID, "Cantidad", 0));
        givenSaveReturnsArgument();

        crearCampo.execute(request("Total", "FORMULA", null, "{" + OTRO_CAMPO_ID + "} * 2"));

        Campo guardado = thenCampoGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoCampo.FORMULA);
        assertThat(guardado.getFormula()).isEqualTo("{" + OTRO_CAMPO_ID + "} * 2");
    }

    @Test
    @DisplayName("Rechaza una fórmula que cita un campo que no es del bloque")
    void executeShouldRejectFormulaWithUnknownField() {
        givenBloqueConCampos(givenCampo(OTRO_CAMPO_ID, "Cantidad", 0));

        assertThatThrownBy(() -> crearCampo.execute(request("Total", "FORMULA", null, "{" + CAMPO_ID + "} * 2")))
                .isInstanceOfSatisfying(CampoInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsEntry("formula", "Usá sólo campos numéricos de esta tabla"));
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza una fórmula que cita un campo que no es numérico")
    void executeShouldRejectFormulaWithNonNumericField() {
        Campo texto = givenCampo(OTRO_CAMPO_ID, "Nombre", 0);
        texto.setTipo(TipoCampo.TEXTO);
        givenBloqueConCampos(texto);

        assertThatThrownBy(() -> crearCampo.execute(request("Total", "FORMULA", null, "{" + OTRO_CAMPO_ID + "} * 2")))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    // --- arrange ---
    private void givenBloqueConCampos(Campo... campos) {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.of(givenBloque()));
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(campos));
    }

    private void givenBloqueRelacionado(ProyectoId proyectoId) {
        Bloque relacionado = Bloque.builder().id(RELACIONADO_ID).negocioId(NEGOCIO_ID).proyectoId(proyectoId).build();
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID)).thenReturn(Optional.of(relacionado));
    }

    private void givenSaveReturnsArgument() {
        when(campoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- assert ---
    private Campo thenCampoGuardado() {
        ArgumentCaptor<Campo> guardado = ArgumentCaptor.forClass(Campo.class);
        verify(campoRepository).save(guardado.capture());
        return guardado.getValue();
    }

    // --- helpers ---
    private CrearCampoRequest request(String nombre, String tipo, BloqueId relacionadoId, String formula) {
        return new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, nombre, tipo, null, null, null, relacionadoId, formula);
    }

    private Bloque givenBloque() {
        return Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).build();
    }

    private Campo givenCampo(CampoId id, String nombre, int posicion) {
        return Campo.builder()
                .id(id)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre(nombre)
                .tipo(TipoCampo.NUMERO)
                .posicion((short) posicion)
                .build();
    }
}
