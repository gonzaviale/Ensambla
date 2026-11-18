package com.ensambla.backend.domain.useCase.receta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.receta.RecetaId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarReceta;
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
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.RecetasDePrueba.AHORA;
import static com.ensambla.backend.support.RecetasDePrueba.AZUCAR_ID;
import static com.ensambla.backend.support.RecetasDePrueba.BLOQUE_INSUMOS_ID;
import static com.ensambla.backend.support.RecetasDePrueba.HARINA_ID;
import static com.ensambla.backend.support.RecetasDePrueba.NEGOCIO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.PRECIO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.PRODUCTO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.RELOJ;
import static com.ensambla.backend.support.RecetasDePrueba.USUARIO_ID;
import static com.ensambla.backend.support.RecetasDePrueba.unosInsumosSinPrecio;
import static com.ensambla.backend.support.RecetasDePrueba.unInsumoSinPrecio;
import static com.ensambla.backend.support.RecetasDePrueba.unPedidoDeGuardarRecetaCon;
import static com.ensambla.backend.support.RecetasDePrueba.unPedidoDeQuitarLaReceta;
import static com.ensambla.backend.support.RecetasDePrueba.unPedidoSinColumnaDelPrecio;
import static com.ensambla.backend.support.RecetasDePrueba.unProducto;
import static com.ensambla.backend.support.RecetasDePrueba.unaColumnaDelPrecioDeTipo;
import static com.ensambla.backend.support.RecetasDePrueba.unaRecetaCosteadaDeHarina;
import static com.ensambla.backend.support.RecetasDePrueba.unaRecetaVacia;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaCasosDeUsoTest {

    private static final RecetaId ID_ANTERIOR = RecetaId.nuevo();

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private ControlPlanService controlPlanService;

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private RecetaService recetaService;

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private CampoRepository campoRepository;

    private ObtenerReceta obtenerReceta;
    private GuardarReceta guardarReceta;
    private RecetaItem receta;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuarioYUnaRecetaCosteadaVacia() {
        obtenerReceta = new ObtenerReceta(
                negocioRepository, controlPlanService, RELOJ, registroRepository, recetaService, recetaRepository);
        guardarReceta = new GuardarReceta(negocioRepository, controlPlanService, recetaService,
                recetaRepository, campoRepository, registroRepository, new TransaccionDirecta(), RELOJ);
        lenient().when(recetaService.costear(any(), eq(NEGOCIO_ID))).thenReturn(RecetaCosteada.vacia());
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    // --- obtener ---

    @Test
    @DisplayName("Obtener devuelve la receta guardada del producto con su costo")
    void obtenerDevuelveLaRecetaGuardadaDelProductoConSuCosto() {
        dadoUnProductoDelNegocio();
        dadoUnaRecetaGuardadaDelProductoConSuCosto();

        cuandoSeObtieneLaRecetaDelProducto();

        entoncesSeDevuelveLaRecetaConSuCosto();
    }

    @Test
    @DisplayName("Obtener costea una receta vacía del producto cuando todavía no tiene una")
    void obtenerCosteaUnaRecetaVaciaDelProductoCuandoTodaviaNoTieneUna() {
        dadoUnProductoDelNegocio();
        dadoQueElProductoNoTieneReceta();

        cuandoSeObtieneLaRecetaDelProducto();

        entoncesSeCosteaUnaRecetaVaciaDelProducto();
    }

    @Test
    @DisplayName("Obtener responde que no existe si el producto es de otro negocio")
    void obtenerRespondeQueNoExisteSiElProductoEsDeOtroNegocio() {
        dadoUnProductoDeOtroNegocio();

        cuandoSeIntentaObtenerLaRecetaDelProducto();

        entoncesSeRechazaPorqueElProductoNoExiste();
    }

    // --- guardar ---

    @Test
    @DisplayName("Guardar arma la receta del producto con la hora del reloj, la guarda y devuelve el costo")
    void guardarArmaLaRecetaDelProductoConLaHoraDelRelojLaGuardaYDevuelveElCosto() {
        dadoUnProductoDelNegocio();
        dadoUnaColumnaDelPrecioDeTipo(TipoCampo.NUMERO);
        dadoQueExistenLosInsumos(HARINA_ID);
        dadoQueLaRecetaSeGuardaTalCual();
        dadoUnaRecetaCosteadaDeHarina();

        cuandoSeGuardaLaRecetaCon(HARINA_ID);

        entoncesSeGuardaLaRecetaDelProductoConLaHoraDelReloj();
        entoncesSeDevuelveElCostoDeLaReceta();
    }

    @Test
    @DisplayName("Guardar reemplaza la receta anterior del producto conservando su identidad")
    void guardarReemplazaLaRecetaAnteriorDelProductoConservandoSuIdentidad() {
        dadoUnProductoDelNegocio();
        dadoUnaColumnaDelPrecioDeTipo(TipoCampo.NUMERO);
        dadoQueExistenLosInsumos(HARINA_ID, AZUCAR_ID);
        dadoUnaRecetaAnteriorDelProducto();
        dadoQueLaRecetaSeGuardaTalCual();

        cuandoSeGuardaLaRecetaCon(HARINA_ID, AZUCAR_ID);

        entoncesLaRecetaGuardadaConservaLaIdentidadDeLaAnterior();
    }

    @Test
    @DisplayName("Guardar sin líneas quita la receta del producto")
    void guardarSinLineasQuitaLaRecetaDelProducto() {
        dadoUnProductoDelNegocio();

        cuandoSeQuitaLaReceta();

        entoncesSeEliminaLaRecetaDelProducto();
        entoncesNoSeGuardaNingunaReceta();
    }

    @Test
    @DisplayName("Guardar rechaza una columna de precio que no existe")
    void guardarRechazaUnaColumnaDePrecioQueNoExiste() {
        dadoUnProductoDelNegocio();
        dadoQueLaColumnaDelPrecioNoExiste();

        cuandoSeIntentaGuardarLaRecetaCon(HARINA_ID);

        entoncesSeRechazaPorLaColumnaDelPrecio();
        entoncesNoSeGuardaNingunaReceta();
    }

    @Test
    @DisplayName("Guardar rechaza una columna de precio que no es numérica")
    void guardarRechazaUnaColumnaDePrecioQueNoEsNumerica() {
        dadoUnProductoDelNegocio();
        dadoUnaColumnaDelPrecioDeTipo(TipoCampo.TEXTO);

        cuandoSeIntentaGuardarLaRecetaCon(HARINA_ID);

        entoncesSeRechazaPorDatosInvalidos();
        entoncesNoSeGuardaNingunaReceta();
    }

    @Test
    @DisplayName("Guardar rechaza un insumo que no es una fila de la tabla del precio")
    void guardarRechazaUnInsumoQueNoEsUnaFilaDeLaTablaDelPrecio() {
        dadoUnProductoDelNegocio();
        dadoUnaColumnaDelPrecioDeTipo(TipoCampo.NUMERO);
        dadoQueSoloExisteElInsumo(HARINA_ID, AZUCAR_ID);

        cuandoSeIntentaGuardarLaRecetaCon(HARINA_ID, AZUCAR_ID);

        entoncesSeRechazaPorElInsumoDeLaSegundaLinea();
        entoncesNoSeGuardaNingunaReceta();
    }

    @Test
    @DisplayName("Guardar rechaza una receta inválida antes de tocar la base")
    void guardarRechazaUnaRecetaInvalidaAntesDeTocarLaBase() {
        dadoUnProductoDelNegocio();

        cuandoSeIntentaGuardarUnaRecetaSinColumnaDelPrecio();

        entoncesSeRechazaPorDatosInvalidos();
        entoncesNoSeGuardaNingunaReceta();
    }

    // --- dado ---
    private void dadoUnProductoDelNegocio() {
        when(registroRepository.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(unProducto());
    }

    private void dadoUnProductoDeOtroNegocio() {
        when(registroRepository.obtener(PRODUCTO_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));
    }

    private void dadoUnaRecetaGuardadaDelProductoConSuCosto() {
        Receta guardada = unaRecetaVacia();
        when(recetaRepository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(guardada));
        when(recetaService.costear(guardada, NEGOCIO_ID)).thenReturn(unaRecetaCosteadaDeHarina());
    }

    private void dadoQueElProductoNoTieneReceta() {
        when(recetaRepository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnaColumnaDelPrecioDeTipo(TipoCampo tipo) {
        when(campoRepository.findByIdAndNegocioId(PRECIO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unaColumnaDelPrecioDeTipo(tipo)));
    }

    private void dadoQueLaColumnaDelPrecioNoExiste() {
        when(campoRepository.findByIdAndNegocioId(PRECIO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoQueExistenLosInsumos(RegistroId... insumos) {
        when(registroRepository.findAllByIdsAndBloque(List.of(insumos), BLOQUE_INSUMOS_ID, NEGOCIO_ID))
                .thenReturn(unosInsumosSinPrecio(insumos));
    }

    private void dadoQueSoloExisteElInsumo(RegistroId existente, RegistroId otro) {
        when(registroRepository.findAllByIdsAndBloque(List.of(existente, otro), BLOQUE_INSUMOS_ID, NEGOCIO_ID))
                .thenReturn(List.of(unInsumoSinPrecio(existente)));
    }

    private void dadoQueLaRecetaSeGuardaTalCual() {
        when(recetaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnaRecetaCosteadaDeHarina() {
        when(recetaService.costear(any(), eq(NEGOCIO_ID))).thenReturn(unaRecetaCosteadaDeHarina());
    }

    private void dadoUnaRecetaAnteriorDelProducto() {
        Receta anterior = Receta.builder().id(ID_ANTERIOR).creadoEn(AHORA.minusDays(1)).build();
        when(recetaRepository.findByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Optional.of(anterior));
    }

    // --- cuando ---
    private void cuandoSeObtieneLaRecetaDelProducto() {
        receta = obtenerReceta.execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID));
    }

    private void cuandoSeIntentaObtenerLaRecetaDelProducto() {
        rechazo = catchThrowable(this::cuandoSeObtieneLaRecetaDelProducto);
    }

    private void cuandoSeGuardaLaRecetaCon(RegistroId... insumos) {
        receta = guardarReceta.execute(unPedidoDeGuardarRecetaCon(insumos));
    }

    private void cuandoSeIntentaGuardarLaRecetaCon(RegistroId... insumos) {
        rechazo = catchThrowable(() -> cuandoSeGuardaLaRecetaCon(insumos));
    }

    private void cuandoSeQuitaLaReceta() {
        guardarReceta.execute(unPedidoDeQuitarLaReceta());
    }

    private void cuandoSeIntentaGuardarUnaRecetaSinColumnaDelPrecio() {
        rechazo = catchThrowable(() -> guardarReceta.execute(unPedidoSinColumnaDelPrecio()));
    }

    // --- entonces ---
    private void entoncesSeDevuelveLaRecetaConSuCosto() {
        assertThat(receta.campoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(receta.bloqueInsumosId()).isEqualTo(BLOQUE_INSUMOS_ID);
        assertThat(receta.costo()).isEqualByComparingTo("30");
        assertThat(receta.incompleta()).isFalse();
        assertThat(receta.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.insumo()).isEqualTo("Harina");
            assertThat(linea.subtotal()).isEqualByComparingTo("30");
        });
    }

    private void entoncesSeCosteaUnaRecetaVaciaDelProducto() {
        Receta costeada = recetaCosteada();
        assertThat(costeada.esVacia()).isTrue();
        assertThat(costeada.getProductoId()).isEqualTo(PRODUCTO_ID);
        assertThat(costeada.getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    private void entoncesSeRechazaPorqueElProductoNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesSeGuardaLaRecetaDelProductoConLaHoraDelReloj() {
        Receta guardada = recetaGuardada();
        assertThat(guardada.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardada.getProductoId()).isEqualTo(PRODUCTO_ID);
        assertThat(guardada.getCampoPrecioId()).isEqualTo(PRECIO_ID);
        assertThat(guardada.getLineas()).containsExactly(new LineaReceta(HARINA_ID, new BigDecimal("2"), "kg"));
        assertThat(guardada.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(guardada.getId()).isNull();
    }

    private void entoncesSeDevuelveElCostoDeLaReceta() {
        assertThat(receta.costo()).isEqualByComparingTo("30");
    }

    private void entoncesLaRecetaGuardadaConservaLaIdentidadDeLaAnterior() {
        Receta guardada = recetaGuardada();
        assertThat(guardada.getId()).isEqualTo(ID_ANTERIOR);
        assertThat(guardada.getCreadoEn()).isEqualTo(AHORA.minusDays(1));
    }

    private void entoncesSeEliminaLaRecetaDelProducto() {
        verify(recetaRepository).deleteByProductoIdAndNegocioId(PRODUCTO_ID, NEGOCIO_ID);
    }

    private void entoncesSeRechazaPorLaColumnaDelPrecio() {
        assertThat(rechazo).isInstanceOfSatisfying(RegistroInvalidoException.class,
                e -> assertThat(e.getErrores()).containsKey("campoPrecioId"));
    }

    private void entoncesSeRechazaPorDatosInvalidos() {
        assertThat(rechazo).isInstanceOf(RegistroInvalidoException.class);
    }

    private void entoncesSeRechazaPorElInsumoDeLaSegundaLinea() {
        assertThat(rechazo).isInstanceOfSatisfying(RegistroInvalidoException.class,
                e -> assertThat(e.getErrores()).containsOnlyKeys("lineas[1]"));
    }

    private void entoncesNoSeGuardaNingunaReceta() {
        verify(recetaRepository, never()).save(any());
    }

    private Receta recetaGuardada() {
        ArgumentCaptor<Receta> guardada = ArgumentCaptor.forClass(Receta.class);
        verify(recetaRepository).save(guardada.capture());
        return guardada.getValue();
    }

    private Receta recetaCosteada() {
        ArgumentCaptor<Receta> costeada = ArgumentCaptor.forClass(Receta.class);
        verify(recetaService).costear(costeada.capture(), eq(NEGOCIO_ID));
        return costeada.getValue();
    }
}
