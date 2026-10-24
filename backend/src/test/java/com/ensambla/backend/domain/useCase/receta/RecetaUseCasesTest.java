package com.ensambla.backend.domain.useCase.receta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.receta.RecetaId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarReceta;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.LineaRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerReceta;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerRecetaRequest;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaUseCasesTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId HARINA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final RegistroId AZUCAR_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    private static final BloqueId BLOQUE_INSUMOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime AHORA = OffsetDateTime.now(RELOJ);

    @Mock
    private NegocioService negocioService;

    @Mock
    private ControlPlanService controlPlanService;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private RecetaService recetaService;

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroRepository registroRepository;

    private ObtenerReceta obtenerReceta;
    private GuardarReceta guardarReceta;

    @BeforeEach
    void setUp() {
        obtenerReceta = new ObtenerReceta(
                negocioService, controlPlanService, RELOJ, registroDatosService, recetaService, recetaRepository);
        guardarReceta = new GuardarReceta(negocioService, controlPlanService, registroDatosService, recetaService,
                recetaRepository, campoRepository, registroRepository, new TransaccionDirecta(), RELOJ);
        lenient().when(recetaService.costear(any(), eq(NEGOCIO_ID))).thenReturn(RecetaCosteada.vacia());
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    // --- obtener ---

    @Test
    @DisplayName("Obtener devuelve la receta guardada del producto con su costo")
    void obtenerShouldReturnRecetaWithCost() {
        Receta receta = Receta.builder().lineas(List.of()).build();
        givenProducto();
        when(recetaRepository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(receta));
        when(recetaService.costear(receta, NEGOCIO_ID)).thenReturn(givenCosteada());

        RecetaItem response = obtenerReceta.execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID));

        assertThat(response.campoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(response.bloqueInsumosId()).isEqualTo(BLOQUE_INSUMOS_ID);
        assertThat(response.costo()).isEqualByComparingTo("30");
        assertThat(response.incompleta()).isFalse();
        assertThat(response.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.insumo()).isEqualTo("Harina");
            assertThat(linea.subtotal()).isEqualByComparingTo("30");
        });
    }

    @Test
    @DisplayName("Obtener costea una receta vacía del producto cuando todavía no tiene una")
    void obtenerShouldCostAnEmptyRecetaWhenThereIsNone() {
        givenProducto();
        when(recetaRepository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
        when(recetaService.costear(any(), eq(NEGOCIO_ID))).thenReturn(RecetaCosteada.vacia());

        obtenerReceta.execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID));

        ArgumentCaptor<Receta> costeada = ArgumentCaptor.forClass(Receta.class);
        verify(recetaService).costear(costeada.capture(), eq(NEGOCIO_ID));
        assertThat(costeada.getValue().esVacia()).isTrue();
        assertThat(costeada.getValue().getProductoId()).isEqualTo(PRODUCTO_ID);
        assertThat(costeada.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Obtener responde 'no existe' si el producto es de otro negocio")
    void obtenerShouldPropagateNotFound() {
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        assertThatThrownBy(() -> obtenerReceta.execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- guardar ---

    @Test
    @DisplayName("Guardar arma la receta del producto con la hora del reloj, la guarda y devuelve el costo")
    void guardarShouldSaveRecetaAndReturnCost() {
        givenProducto();
        givenColumnaDelPrecio(TipoCampo.NUMERO);
        givenInsumosExistentes(HARINA_ID);
        givenSaveReturnsArgument();
        when(recetaService.costear(any(), eq(NEGOCIO_ID))).thenReturn(givenCosteada());

        RecetaItem response = guardarReceta.execute(givenGuardar(HARINA_ID));

        Receta guardada = thenGuardada();
        assertThat(guardada.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardada.getProductoId()).isEqualTo(PRODUCTO_ID);
        assertThat(guardada.getCampoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(guardada.getLineas()).containsExactly(new LineaReceta(HARINA_ID, new BigDecimal("2"), "kg"));
        assertThat(guardada.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(guardada.getId()).isNull();
        assertThat(response.costo()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("Guardar reemplaza la receta anterior del producto conservando su identidad")
    void guardarShouldKeepTheIdentityOfThePreviousOne() {
        Receta anterior = Receta.builder().id(RecetaId.nuevo()).creadoEn(AHORA.minusDays(1)).build();
        givenProducto();
        givenColumnaDelPrecio(TipoCampo.NUMERO);
        givenInsumosExistentes(HARINA_ID, AZUCAR_ID);
        when(recetaRepository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(anterior));
        givenSaveReturnsArgument();

        guardarReceta.execute(givenGuardar(HARINA_ID, AZUCAR_ID));

        Receta guardada = thenGuardada();
        assertThat(guardada.getId()).isEqualTo(anterior.getId());
        assertThat(guardada.getCreadoEn()).isEqualTo(anterior.getCreadoEn());
    }

    @Test
    @DisplayName("Guardar sin líneas quita la receta del producto")
    void guardarShouldDeleteRecetaWhenThereAreNoLines() {
        givenProducto();

        guardarReceta.execute(new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, null, List.of()));

        verify(recetaRepository).deleteByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID);
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guardar rechaza una columna de precio que no existe")
    void guardarShouldRejectMissingPriceColumn() {
        givenProducto();
        when(campoRepository.findByIdAndNegocioId(PRECIO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guardarReceta.execute(givenGuardar(HARINA_ID)))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsKey("campoPrecioId"));
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guardar rechaza una columna de precio que no es numérica")
    void guardarShouldRejectNonNumericPriceColumn() {
        givenProducto();
        givenColumnaDelPrecio(TipoCampo.TEXTO);

        assertThatThrownBy(() -> guardarReceta.execute(givenGuardar(HARINA_ID)))
                .isInstanceOf(RegistroInvalidoException.class);
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guardar rechaza un insumo que no es una fila de la tabla del precio")
    void guardarShouldRejectInsumoThatIsNotInThePriceTable() {
        givenProducto();
        givenColumnaDelPrecio(TipoCampo.NUMERO);
        when(registroRepository.findAllByIdsAndBloque(List.of(HARINA_ID, AZUCAR_ID), BLOQUE_INSUMOS_ID, NEGOCIO_ID))
                .thenReturn(List.of(givenInsumo(HARINA_ID)));

        assertThatThrownBy(() -> guardarReceta.execute(givenGuardar(HARINA_ID, AZUCAR_ID)))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsOnlyKeys("lineas[1]"));
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guardar rechaza una receta inválida antes de tocar la base")
    void guardarShouldRejectInvalidRecetaFirst() {
        givenProducto();
        GuardarRecetaRequest invalida = new GuardarRecetaRequest(
                USUARIO_ID, PRODUCTO_ID, null, List.of(new LineaRecetaRequest(HARINA_ID, BigDecimal.ONE, null)));

        assertThatThrownBy(() -> guardarReceta.execute(invalida)).isInstanceOf(RegistroInvalidoException.class);
        verify(recetaRepository, never()).save(any());
    }

    // --- arrange ---
    private void givenProducto() {
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Registro.builder().id(PRODUCTO_ID).build());
    }

    private void givenColumnaDelPrecio(TipoCampo tipo) {
        when(campoRepository.findByIdAndNegocioId(PRECIO_ID, NEGOCIO_ID)).thenReturn(Optional.of(
                Campo.builder().id(PRECIO_ID).bloqueId(BLOQUE_INSUMOS_ID).tipo(tipo).build()));
    }

    private void givenInsumosExistentes(RegistroId... insumos) {
        when(registroRepository.findAllByIdsAndBloque(List.of(insumos), BLOQUE_INSUMOS_ID, NEGOCIO_ID))
                .thenReturn(Arrays.stream(insumos).map(RecetaUseCasesTest::givenInsumo).toList());
    }

    private void givenSaveReturnsArgument() {
        when(recetaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Registro givenInsumo(RegistroId id) {
        return Registro.builder().id(id).valores(Map.of()).build();
    }

    private static GuardarRecetaRequest givenGuardar(RegistroId... insumos) {
        return new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, PRECIO_ID, Arrays.stream(insumos)
                .map(insumo -> new LineaRecetaRequest(insumo, new BigDecimal("2"), "kg"))
                .toList());
    }

    private static RecetaCosteada givenCosteada() {
        return new RecetaCosteada(
                BLOQUE_INSUMOS_ID,
                PRECIO_ID,
                List.of(new RecetaCosteada.Linea(
                        HARINA_ID, "Harina", new BigDecimal("3"), "kg", BigDecimal.TEN, new BigDecimal("30"))),
                new BigDecimal("30"),
                false);
    }

    // --- assert ---
    private Receta thenGuardada() {
        ArgumentCaptor<Receta> guardada = ArgumentCaptor.forClass(Receta.class);
        verify(recetaRepository).save(guardada.capture());
        return guardada.getValue();
    }
}
