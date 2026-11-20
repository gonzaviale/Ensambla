package com.ensambla.backend.domain.useCase.inventario;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStock;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockRequest;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockResponse;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientos;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientosRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimiento;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoResponse;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioCasosDeUsoTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId OTRO_BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private BloqueRepository bloqueRepository;

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroRepository registroRepository;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MovimientoStockRepository movimientoStockRepository;

    private RegistrarMovimiento registrarMovimiento;
    private ListarMovimientos listarMovimientos;
    private ConsultarStock consultarStock;

    private RegistrarMovimientoResponse respuestaDeRegistro;
    private List<MovimientoItem> movimientosListados;
    private ConsultarStockResponse respuestaDeStock;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        registrarMovimiento = new RegistrarMovimiento(negocioRepository, bloqueRepository, campoRepository,
                registroRepository, movimientoStockRepository, new TransaccionDirecta(), RELOJ);
        listarMovimientos = new ListarMovimientos(
                negocioRepository, bloqueRepository, campoRepository, registroRepository, movimientoStockRepository);
        consultarStock = new ConsultarStock(negocioRepository, bloqueRepository, campoRepository, registroRepository,
                movimientoStockRepository);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Registrar guarda el movimiento del producto del bloque y devuelve el stock resultante")
    void registrarGuardaElMovimientoDelProductoYDevuelveElStockResultante() {
        dadoUnProductoDelBloqueConStockDe("12");

        cuandoSeRegistra("ENTRADA", "2", " Compra ");

        entoncesSeGuardoElMovimientoDelNegocioConNota("Compra");
        entoncesLaRespuestaMuestraLaEntradaDeHarinaConStockDe("12");
    }

    @Test
    @DisplayName("Registrar rechaza un movimiento inválido antes de buscar nada")
    void registrarRechazaUnMovimientoInvalidoAntesDeBuscarNada() {
        cuandoSeRegistraUnMovimientoSinProducto();

        entoncesSeRechazaComoRegistroInvalido();
        entoncesNoSeBuscoNada();
    }

    @Test
    @DisplayName("Registrar responde 'no existe' si el producto es de otro bloque")
    void registrarRespondeNoExisteSiElProductoEsDeOtroBloque() {
        dadoUnProductoQueEsDeOtroBloque();

        cuandoSeRegistra("ENTRADA", "1", null);

        entoncesNoSeEncuentraElProducto();
        entoncesNoSeGuardaNingunMovimiento();
    }

    @Test
    @DisplayName("Listar devuelve los movimientos con el nombre de su producto")
    void listarDevuelveLosMovimientosConElNombreDeSuProducto() {
        dadoUnaSalidaRegistradaDeHarina();

        cuandoSeListanLosMovimientosDelProducto();

        entoncesSeListaUnaSalidaDeHarina();
    }

    @Test
    @DisplayName("Stock consulta una página de productos con la misma búsqueda que el listado y su stock")
    void stockConsultaUnaPaginaDeProductosConLaBusquedaDelListadoYSuStock() {
        dadoUnaPaginaConHarinaYStockDe("4");

        cuandoSeConsultaElStockBuscando("har");

        entoncesSeBuscoPorTextoEnElNegocio("har");
        entoncesLaPaginaMuestraHarinaConStockDe("4");
    }

    @Test
    @DisplayName("Registrar una entrada la guarda sin consultar el stock antes")
    void registrarUnaEntradaLaGuardaSinConsultarElStockAntes() {
        dadoUnProductoDelBloqueConStockDe("7");

        cuandoSeRegistra("ENTRADA", "5", null);

        entoncesSeGuardaElMovimientoYDespuesSeConsultaElStock();
    }

    @Test
    @DisplayName("Registrar una salida menor o igual al stock la guarda")
    void registrarUnaSalidaMenorOIgualAlStockLaGuarda() {
        dadoUnProductoDelBloqueConStockDe("5");

        cuandoSeRegistra("SALIDA", "5", null);

        entoncesLaRespuestaEsUnMovimientoDeTipo("SALIDA");
    }

    @Test
    @DisplayName("Registrar una salida mayor al stock la rechaza y avisa cuánto queda")
    void registrarUnaSalidaMayorAlStockLaRechazaYAvisaCuantoQueda() {
        dadoUnProductoConStockSinCamposDe("2.500");

        cuandoSeRegistra("SALIDA", "3", null);

        entoncesSeRechazaPorFaltaDeStockQuedando("2.5");
        entoncesNoSeGuardaNingunMovimiento();
    }

    @Test
    @DisplayName("Registrar la salida de un producto sin movimientos la rechaza: no hay stock")
    void registrarLaSalidaDeUnProductoSinMovimientosLaRechazaPorqueNoHayStock() {
        dadoUnProductoSinMovimientos();

        cuandoSeRegistra("SALIDA", "1", null);

        entoncesSeRechazaPorFaltaDeStockQuedando("0");
    }

    // --- dado ---

    private void dadoUnProductoDelBloqueConStockDe(String stock) {
        dadoElBloqueConSuProducto();
        dadoQueElBloqueTieneSusCampos();
        dadoQueGuardarDevuelveElMovimiento();
        dadoElStock(stock);
    }

    private void dadoUnProductoQueEsDeOtroBloque() {
        dadoElBloqueConSuProducto(OTRO_BLOQUE_ID);
    }

    private void dadoUnProductoConStockSinCamposDe(String stock) {
        dadoElBloqueConSuProducto();
        dadoElStock(stock);
    }

    private void dadoUnProductoSinMovimientos() {
        dadoElBloqueConSuProducto();
        when(movimientoStockRepository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID)).thenReturn(Map.of());
    }

    private void dadoUnaSalidaRegistradaDeHarina() {
        dadoElBloqueConSusCampos();
        when(movimientoStockRepository.findRecientes(BLOQUE_ID, NEGOCIO_ID, PRODUCTO_ID, 100))
                .thenReturn(List.of(unaSalidaDeHarina()));
        when(registroRepository.findAllByIdsAndBloque(List.of(PRODUCTO_ID), BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(unProductoDelBloque(BLOQUE_ID)));
    }

    private void dadoUnaPaginaConHarinaYStockDe(String stock) {
        dadoElBloqueConSusCampos();
        when(registroRepository.buscar(any()))
                .thenReturn(new Pagina<>(List.of(unProductoDelBloque(BLOQUE_ID)), 0, 20, 1));
        dadoElStock(stock);
    }

    private void dadoElBloqueConSuProducto() {
        dadoElBloqueConSuProducto(BLOQUE_ID);
    }

    private void dadoElBloqueConSuProducto(BloqueId bloqueDelProducto) {
        dadoQueExisteElBloque();
        when(registroRepository.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(unProductoDelBloque(bloqueDelProducto));
    }

    private void dadoElBloqueConSusCampos() {
        dadoQueExisteElBloque();
        dadoQueElBloqueTieneSusCampos();
    }

    private void dadoQueExisteElBloque() {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
    }

    private void dadoQueElBloqueTieneSusCampos() {
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(unosCampos());
    }

    private void dadoQueGuardarDevuelveElMovimiento() {
        when(movimientoStockRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoElStock(String stock) {
        when(movimientoStockRepository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID))
                .thenReturn(Map.of(PRODUCTO_ID, new BigDecimal(stock)));
    }

    // --- cuando ---

    private void cuandoSeRegistra(String tipo, String cantidad, String nota) {
        rechazo = catchThrowable(() -> respuestaDeRegistro = registrarMovimiento.execute(
                new RegistrarMovimientoRequest(
                        USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, tipo, new BigDecimal(cantidad), null, nota)));
    }

    private void cuandoSeRegistraUnMovimientoSinProducto() {
        rechazo = catchThrowable(() -> registrarMovimiento.execute(new RegistrarMovimientoRequest(
                USUARIO_ID, BLOQUE_ID, null, "ENTRADA", BigDecimal.ONE, null, null)));
    }

    private void cuandoSeListanLosMovimientosDelProducto() {
        movimientosListados = listarMovimientos.execute(
                new ListarMovimientosRequest(USUARIO_ID, BLOQUE_ID, PRODUCTO_ID));
    }

    private void cuandoSeConsultaElStockBuscando(String texto) {
        respuestaDeStock = consultarStock.execute(new ConsultarStockRequest(USUARIO_ID, BLOQUE_ID, texto, 0, 20));
    }

    // --- entonces ---

    private void entoncesSeGuardoElMovimientoDelNegocioConNota(String nota) {
        MovimientoStock guardado = elMovimientoGuardado();
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getFecha()).isEqualTo(LocalDate.parse("2026-10-13"));
        assertThat(guardado.getNota()).isEqualTo(nota);
    }

    private void entoncesLaRespuestaMuestraLaEntradaDeHarinaConStockDe(String stock) {
        assertThat(respuestaDeRegistro.movimiento().producto()).isEqualTo("Harina");
        assertThat(respuestaDeRegistro.movimiento().tipo()).isEqualTo("ENTRADA");
        assertThat(respuestaDeRegistro.stockActual()).isEqualByComparingTo(stock);
    }

    private void entoncesLaRespuestaEsUnMovimientoDeTipo(String tipo) {
        assertThat(respuestaDeRegistro.movimiento().tipo()).isEqualTo(tipo);
    }

    private void entoncesSeRechazaComoRegistroInvalido() {
        assertThat(rechazo).isInstanceOf(RegistroInvalidoException.class);
    }

    private void entoncesNoSeBuscoNada() {
        verifyNoInteractions(bloqueRepository, registroRepository, movimientoStockRepository);
    }

    private void entoncesNoSeEncuentraElProducto() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardaNingunMovimiento() {
        verify(movimientoStockRepository, never()).save(any());
    }

    private void entoncesSeListaUnaSalidaDeHarina() {
        assertThat(movimientosListados).singleElement().satisfies(item -> {
            assertThat(item.producto()).isEqualTo("Harina");
            assertThat(item.tipo()).isEqualTo("SALIDA");
        });
    }

    private void entoncesSeBuscoPorTextoEnElNegocio(String texto) {
        ArgumentCaptor<ConsultaRegistros> consulta = ArgumentCaptor.forClass(ConsultaRegistros.class);
        verify(registroRepository).buscar(consulta.capture());
        assertThat(consulta.getValue().texto()).isEqualTo(texto);
        assertThat(consulta.getValue().negocioId()).isEqualTo(NEGOCIO_ID);
    }

    private void entoncesLaPaginaMuestraHarinaConStockDe(String stock) {
        assertThat(respuestaDeStock.contenido()).singleElement().satisfies(item -> {
            assertThat(item.producto()).isEqualTo("Harina");
            assertThat(item.stock()).isEqualByComparingTo(stock);
        });
    }

    private void entoncesSeGuardaElMovimientoYDespuesSeConsultaElStock() {
        InOrder orden = inOrder(movimientoStockRepository);
        orden.verify(movimientoStockRepository).save(any());
        orden.verify(movimientoStockRepository).stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID);
    }

    private void entoncesSeRechazaPorFaltaDeStockQuedando(String cantidad) {
        assertThat(rechazo).isInstanceOfSatisfying(RegistroInvalidoException.class, e -> assertThat(e.getErrores())
                .containsEntry("cantidad", "No hay stock suficiente: quedan " + cantidad));
    }

    // --- datos ---

    private MovimientoStock elMovimientoGuardado() {
        ArgumentCaptor<MovimientoStock> guardado = ArgumentCaptor.forClass(MovimientoStock.class);
        verify(movimientoStockRepository).save(guardado.capture());
        return guardado.getValue();
    }

    private static MovimientoStock unaSalidaDeHarina() {
        return MovimientoStock.builder().registroId(PRODUCTO_ID).tipo(TipoMovimiento.SALIDA)
                .cantidad(BigDecimal.ONE).build();
    }

    private static Registro unProductoDelBloque(BloqueId bloqueId) {
        return Registro.builder()
                .id(PRODUCTO_ID)
                .bloqueId(bloqueId)
                .valores(Map.of(CAMPO_NOMBRE_ID.toString(), "Harina"))
                .build();
    }

    private static List<Campo> unosCampos() {
        return List.of(Campo.builder().id(CAMPO_NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build());
    }
}
