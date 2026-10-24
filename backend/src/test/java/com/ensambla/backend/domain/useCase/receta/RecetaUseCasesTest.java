package com.ensambla.backend.domain.useCase.receta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarReceta;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.LineaRecetaRequest;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerReceta;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerRecetaRequest;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaUseCasesTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID HARINA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID BLOQUE_INSUMOS_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final UUID PRECIO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000002");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NegocioService negocioService;
    @Mock
    private ControlPlanService controlPlanService;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private RecetaService recetaService;

    private ObtenerReceta obtenerReceta;
    private GuardarReceta guardarReceta;

    @BeforeEach
    void setUp() {
        obtenerReceta = new ObtenerReceta(
                negocioService, controlPlanService, RELOJ, registroDatosService, recetaService);
        guardarReceta = new GuardarReceta(
                negocioService, controlPlanService, registroDatosService, recetaService, RELOJ);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Obtener devuelve la receta del producto con su costo")
    void obtenerShouldReturnRecetaWithCost() {
        Receta receta = Receta.builder().lineas(List.of()).build();
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(givenProducto());
        when(recetaService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(receta);
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
    @DisplayName("Obtener responde 'no existe' si el producto es de otro negocio")
    void obtenerShouldPropagateNotFound() {
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        assertThatThrownBy(() -> obtenerReceta.execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Guardar arma la receta del producto, la guarda y devuelve el costo")
    void guardarShouldSaveRecetaAndReturnCost() {
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(givenProducto());
        when(recetaService.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recetaService.costear(any(), eq(NEGOCIO_ID))).thenReturn(givenCosteada());

        RecetaItem response = guardarReceta.execute(new GuardarRecetaRequest(
                USUARIO_ID, PRODUCTO_ID, PRECIO_ID, List.of(new LineaRecetaRequest(HARINA_ID, new BigDecimal("3"), "kg"))));

        ArgumentCaptor<Receta> guardada = ArgumentCaptor.forClass(Receta.class);
        verify(recetaService).guardar(guardada.capture());
        assertThat(guardada.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardada.getValue().getProductoId()).isEqualTo(PRODUCTO_ID);
        assertThat(guardada.getValue().getCampoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(guardada.getValue().getLineas())
                .containsExactly(new LineaReceta(HARINA_ID, new BigDecimal("3"), "kg"));
        assertThat(guardada.getValue().getActualizadoEn()).isEqualTo(RELOJ.instant().atOffset(ZoneOffset.UTC));
        assertThat(response.costo()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("Guardar rechaza una receta inválida antes de tocar la base")
    void guardarShouldRejectInvalidRecetaFirst() {
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(givenProducto());
        GuardarRecetaRequest invalida = new GuardarRecetaRequest(
                USUARIO_ID, PRODUCTO_ID, null, List.of(new LineaRecetaRequest(HARINA_ID, BigDecimal.ONE, null)));

        assertThatThrownBy(() -> guardarReceta.execute(invalida)).isInstanceOf(RegistroInvalidoException.class);
        verify(recetaService, never()).guardar(any());
    }

    private static Registro givenProducto() {
        return Registro.builder().id(PRODUCTO_ID).build();
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
}
