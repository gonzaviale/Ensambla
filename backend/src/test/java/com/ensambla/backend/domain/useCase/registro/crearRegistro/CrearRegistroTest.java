package com.ensambla.backend.domain.useCase.registro.crearRegistro;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearRegistroTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final BloqueId CLIENTES_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId CLIENTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId GARANTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final RegistroId ANA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final RegistroId BETO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    private static final Instant INSTANTE = Instant.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    @Mock
    private NegocioService negocioService;

    @Mock
    private BloqueService bloqueService;

    @Mock
    private CampoService campoService;

    @Mock
    private ControlPlanService controlPlanService;

    @Mock
    private RegistroRepository registroRepository;

    private CrearRegistro crearRegistro;

    @BeforeEach
    void setUp() {
        RegistroDatosService registroDatosService =
                new RegistroDatosService(registroRepository, new TransaccionDirecta());
        crearRegistro = new CrearRegistro(negocioService, bloqueService, campoService, registroDatosService,
                controlPlanService, registroRepository, new TransaccionDirecta(),
                Clock.fixed(INSTANTE, ZoneOffset.UTC));
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Crea en el bloque del negocio de la sesión el registro con los valores validados y lo devuelve")
    void executeShouldSaveRegistroWithValidatedValues() {
        givenBloqueConCampos(campos());
        givenSaveReturnsArgument();

        CrearRegistroResponse response = crear(Map.of(NOMBRE_ID.toString(), "  Harina ", "ajeno", 1));

        Registro guardado = thenGuardado();
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(guardado.getValores()).containsOnly(Map.entry(NOMBRE_ID.toString(), "Harina"));
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(response.registro().valores()).containsOnly(Map.entry(NOMBRE_ID.toString(), "Harina"));
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
    }

    @Test
    @DisplayName("No guarda nada cuando los valores no son válidos")
    void executeShouldNotSaveWhenValuesAreInvalid() {
        givenBloqueConCampos(campos());

        assertThatThrownBy(() -> crear(Map.of(CANTIDAD_ID.toString(), 3)))
                .isInstanceOf(RegistroInvalidoException.class);

        verifyNoInteractions(registroRepository);
    }

    @Test
    @DisplayName("Crea el registro cuando la fila a la que apunta la conexión existe")
    void executeShouldSaveWhenReferencedRowExists() {
        givenBloqueConCampos(conexiones());
        givenFilasExistentes(Set.of(ANA_ID), ANA_ID);
        givenSaveReturnsArgument();

        CrearRegistroResponse response = crear(valoresCliente(ANA_ID));

        assertThat(response.registro().valores()).containsEntry(CLIENTE_ID.toString(), ANA_ID.toString());
    }

    @Test
    @DisplayName("Rechaza el registro cuando la fila a la que apunta la conexión ya no existe")
    void executeShouldRejectWhenReferencedRowIsMissing() {
        givenBloqueConCampos(conexiones());
        givenFilasExistentes(Set.of(ANA_ID));

        assertThatThrownBy(() -> crear(valoresCliente(ANA_ID)))
                .isInstanceOf(RegistroInvalidoException.class)
                .extracting(error -> ((RegistroInvalidoException) error).getErrores())
                .isEqualTo(Map.of(CLIENTE_ID.toString(), "Esa fila ya no existe"));
        verify(registroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Busca de una vez las filas de todas las conexiones a la misma tabla y marca sólo las que faltan")
    void executeShouldQueryOncePerBloqueAndReportOnlyMissingRows() {
        givenBloqueConCampos(conexiones());
        givenFilasExistentes(Set.of(ANA_ID, BETO_ID), ANA_ID);

        assertThatThrownBy(() -> crear(Map.of(CLIENTE_ID.toString(), ANA_ID.toString(), GARANTE_ID.toString(),
                BETO_ID.toString())))
                .isInstanceOf(RegistroInvalidoException.class)
                .extracting(error -> ((RegistroInvalidoException) error).getErrores())
                .isEqualTo(Map.of(GARANTE_ID.toString(), "Esa fila ya no existe"));
    }

    @Test
    @DisplayName("No consulta otras tablas cuando las conexiones quedan vacías")
    void executeShouldNotQueryWhenConexionesAreEmpty() {
        givenBloqueConCampos(conexiones());
        givenSaveReturnsArgument();

        crear(Map.of());

        verify(registroRepository, never()).findAllByIdsAndBloque(any(), any(), any());
    }

    @Test
    @DisplayName("No valida ni guarda cuando el bloque no es del negocio")
    void executeShouldPropagateNotFoundWhenBloqueIsNotOfTheNegocio() {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));

        assertThatThrownBy(() -> crear(Map.of())).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(campoService, registroRepository);
    }

    // --- arrange ---
    private void givenBloqueConCampos(List<Campo> campos) {
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(campos);
    }

    private void givenFilasExistentes(Set<RegistroId> buscados, RegistroId... existentes) {
        List<Registro> filas = List.of(existentes).stream()
                .map(id -> Registro.builder().id(id).bloqueId(CLIENTES_ID).build())
                .toList();
        when(registroRepository.findAllByIdsAndBloque(buscados, CLIENTES_ID, NEGOCIO_ID)).thenReturn(filas);
    }

    private void givenSaveReturnsArgument() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static List<Campo> conexiones() {
        return List.of(
                Campo.builder().id(CLIENTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(GARANTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).build());
    }

    private static Map<String, Object> valoresCliente(RegistroId filaId) {
        return Map.of(CLIENTE_ID.toString(), filaId.toString());
    }

    private static List<Campo> campos() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).obligatorio(true).build(),
                Campo.builder().id(CANTIDAD_ID).tipo(TipoCampo.NUMERO).obligatorio(false).build());
    }

    // --- act ---
    private CrearRegistroResponse crear(Map<String, Object> valores) {
        return crearRegistro.execute(new CrearRegistroRequest(USUARIO_ID, BLOQUE_ID, valores));
    }

    // --- assert ---
    private Registro thenGuardado() {
        ArgumentCaptor<Registro> guardado = ArgumentCaptor.forClass(Registro.class);
        verify(registroRepository).save(guardado.capture());
        return guardado.getValue();
    }
}
