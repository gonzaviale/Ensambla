package com.ensambla.backend.domain.service.registro;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.DireccionOrden;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.precio.HistorialPrecioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroDatosServiceTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000005");
    private static final BloqueId CLIENTES_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId CLIENTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId GARANTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final RegistroId BETO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime ANTES = OffsetDateTime.parse("2026-10-09T12:00:00Z");

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private BloqueRepository bloqueRepository;

    @Mock
    private HistorialPrecioRepository historialPrecioRepository;

    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

    @InjectMocks
    private RegistroDatosService registroDatosService;

    private Registro registro;
    private Registro editado;
    private Throwable rechazo;
    private List<Registro> encontrados;
    private List<Registro> referenciables;

    @Test
    @DisplayName("Edita sólo lo enviado, conserva el resto y actualiza la fecha de modificación")
    void editaSoloLoEnviadoConservaElRestoYActualizaLaFechaDeModificacion() {
        dadoQueElRepositorioGuardaLoQueRecibe();
        dadoUnRegistroDeHarinaConTresUnidades();

        cuandoSeCambiaLaCantidadANueve();

        entoncesSeConservaElRestoYSeActualizaLaFechaDeModificacion();
    }

    @Test
    @DisplayName("Anota el cambio de precio cuando se edita el precio de un producto del inventario")
    void anotaElCambioDePrecioCuandoSeEditaElPrecioDeUnProductoDelInventario() {
        dadoUnBloqueDeInventario();
        dadoUnProductoConPrecio(1000);

        cuandoSeEditaElPrecioA(1200);

        entoncesSeAnotaElCambioDePrecio(new BigDecimal("1000"), new BigDecimal("1200"));
    }

    @Test
    @DisplayName("Anota el primer precio sin precio anterior")
    void anotaElPrimerPrecioSinPrecioAnterior() {
        dadoUnBloqueDeInventario();
        dadoUnRegistroDeHarinaConTresUnidades();

        cuandoSeEditaElPrecioA(1200);

        entoncesSeAnotaElCambioDePrecio(null, new BigDecimal("1200"));
    }

    @Test
    @DisplayName("No anota nada cuando el precio no cambia")
    void noAnotaNadaCuandoElPrecioNoCambia() {
        dadoQueElRepositorioGuardaLoQueRecibe();
        dadoUnProductoConPrecio(1000);

        cuandoSeEditaElPrecioA(1000);

        entoncesNoSeAnotaNingunCambioDePrecio();
    }

    @Test
    @DisplayName("No anota nada cuando se borra el precio")
    void noAnotaNadaCuandoSeBorraElPrecio() {
        dadoQueElRepositorioGuardaLoQueRecibe();
        dadoUnProductoConPrecio(1000);

        cuandoSeBorraElPrecio();

        entoncesNoSeAnotaNingunCambioDePrecio();
    }

    @Test
    @DisplayName("No anota el precio de una fila de ventas: sólo cuenta el del inventario")
    void noAnotaElPrecioDeUnaFilaDeVentasSoloCuentaElDelInventario() {
        dadoUnBloqueDeVentas();
        dadoUnProductoConPrecio(1000);

        cuandoSeEditaElPrecioA(1200);

        entoncesNoSeAnotaNingunCambioDePrecio();
    }

    @Test
    @DisplayName("No anota nada cuando el bloque del registro ya no existe")
    void noAnotaNadaCuandoElBloqueDelRegistroYaNoExiste() {
        dadoQueElBloqueYaNoExiste();
        dadoUnProductoConPrecio(1000);

        cuandoSeEditaElPrecioA(1200);

        entoncesNoSeAnotaNingunCambioDePrecio();
    }

    @Test
    @DisplayName("No guarda la edición cuando un valor nuevo no es válido")
    void noGuardaLaEdicionCuandoUnValorNuevoNoEsValido() {
        dadoUnRegistroDeHarinaConTresUnidades();

        cuandoSeIntentaCambiarLaCantidadAUnTexto();

        entoncesSeRechazaSinGuardarNiCambiarElRegistro();
    }

    @Test
    @DisplayName("No guarda la edición cuando la conexión apunta a una fila que ya no existe")
    void noGuardaLaEdicionCuandoLaConexionApuntaAUnaFilaQueYaNoExiste() {
        dadoUnRegistroDeHarinaConTresUnidades();
        dadoQueLaFilaConectadaYaNoExiste();

        cuandoSeIntentaConectarConLaFilaInexistente();

        entoncesSeRechazaSinGuardar();
    }

    @Test
    @DisplayName("Las referencias pedidas por id salen del repositorio filtradas por bloque y negocio")
    void lasReferenciasPedidasPorIdSalenDelRepositorioFiltradasPorBloqueYNegocio() {
        dadoReferenciasEncontradasPorId();

        cuandoSePidenLasReferenciasPorId();

        entoncesSeDevuelvenLasEncontradasPorId();
    }

    @Test
    @DisplayName("Sin ids busca por texto las 20 filas más nuevas del bloque")
    void sinIdsBuscaPorTextoLas20FilasMasNuevasDelBloque() {
        dadoUnaBusquedaPorTextoConUnaFila();

        cuandoSePidenLasReferenciasPorTexto();

        entoncesSeDevuelveLaFilaEncontrada();
    }

    // --- dado ---

    private void dadoQueElRepositorioGuardaLoQueRecibe() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnRegistroDeHarinaConTresUnidades() {
        registro = unRegistro();
    }

    private void dadoUnProductoConPrecio(Object precio) {
        registro = unRegistroConPrecio(precio);
    }

    private void dadoUnBloqueDeInventario() {
        dadoUnBloqueDe(TipoBloque.INVENTARIO);
    }

    private void dadoUnBloqueDeVentas() {
        dadoUnBloqueDe(TipoBloque.VENTAS);
    }

    private void dadoUnBloqueDe(TipoBloque tipo) {
        dadoQueElRepositorioGuardaLoQueRecibe();
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(Bloque.builder().id(BLOQUE_ID).tipo(tipo).build()));
    }

    private void dadoQueElBloqueYaNoExiste() {
        dadoQueElRepositorioGuardaLoQueRecibe();
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoQueLaFilaConectadaYaNoExiste() {
        when(registroRepository.findAllByIdsAndBloque(Set.of(BETO_ID), CLIENTES_ID, NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoReferenciasEncontradasPorId() {
        encontrados = List.of(unRegistro());
        when(registroRepository.findAllByIdsAndBloque(List.of(REGISTRO_ID), BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(encontrados);
    }

    private void dadoUnaBusquedaPorTextoConUnaFila() {
        ConsultaRegistros consulta = new ConsultaRegistros(
                NEGOCIO_ID, BLOQUE_ID, "ana", null, DireccionOrden.DESC, 0, 20);
        when(registroRepository.buscar(consulta)).thenReturn(new Pagina<>(List.of(unRegistro()), 0, 20, 1));
    }

    // --- cuando ---

    private void cuandoSeCambiaLaCantidadANueve() {
        editado = registroDatosService.editar(registro, camposDelInventario(),
                Map.of(CANTIDAD_ID.toString(), 9), AHORA);
    }

    private void cuandoSeEditaElPrecioA(Object precio) {
        editado = registroDatosService.editar(registro, camposConPrecio(), Map.of(PRECIO_ID.toString(), precio), AHORA);
    }

    private void cuandoSeBorraElPrecio() {
        editado = registroDatosService.editar(registro, camposConPrecio(),
                Collections.singletonMap(PRECIO_ID.toString(), null), AHORA);
    }

    private void cuandoSeIntentaCambiarLaCantidadAUnTexto() {
        rechazo = catchThrowable(() -> registroDatosService.editar(registro, camposDelInventario(),
                Map.of(CANTIDAD_ID.toString(), "muchos"), AHORA));
    }

    private void cuandoSeIntentaConectarConLaFilaInexistente() {
        rechazo = catchThrowable(() -> registroDatosService.editar(registro, conexiones(),
                Map.of(CLIENTE_ID.toString(), BETO_ID.toString()), AHORA));
    }

    private void cuandoSePidenLasReferenciasPorId() {
        referenciables = registroDatosService.buscarReferenciables(
                NEGOCIO_ID, BLOQUE_ID, camposDelInventario(), "ignorado", List.of(REGISTRO_ID));
    }

    private void cuandoSePidenLasReferenciasPorTexto() {
        referenciables = registroDatosService.buscarReferenciables(
                NEGOCIO_ID, BLOQUE_ID, camposDelInventario(), " ana ", List.of());
    }

    // --- entonces ---

    private void entoncesSeConservaElRestoYSeActualizaLaFechaDeModificacion() {
        assertThat(editado.getValores())
                .containsEntry(NOMBRE_ID.toString(), "Harina")
                .containsEntry(CANTIDAD_ID.toString(), 9);
        assertThat(editado.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(editado.getCreadoEn()).isEqualTo(ANTES);
    }

    private void entoncesSeAnotaElCambioDePrecio(BigDecimal anterior, BigDecimal nuevo) {
        ArgumentCaptor<CambioPrecio> captor = ArgumentCaptor.forClass(CambioPrecio.class);
        verify(historialPrecioRepository).save(captor.capture());
        CambioPrecio cambio = captor.getValue();
        assertThat(cambio.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(cambio.registroId()).isEqualTo(REGISTRO_ID);
        assertThat(cambio.anterior()).isEqualTo(anterior);
        assertThat(cambio.nuevo()).isEqualTo(nuevo);
        assertThat(cambio.cambiadoEn()).isEqualTo(AHORA);
    }

    private void entoncesNoSeAnotaNingunCambioDePrecio() {
        verifyNoInteractions(historialPrecioRepository);
    }

    private void entoncesSeRechazaSinGuardarNiCambiarElRegistro() {
        assertThat(rechazo).isInstanceOf(RegistroInvalidoException.class);
        verifyNoMoreInteractions(registroRepository);
        assertThat(registro.getValores()).containsEntry(CANTIDAD_ID.toString(), 3);
    }

    private void entoncesSeRechazaSinGuardar() {
        assertThat(rechazo).isInstanceOf(RegistroInvalidoException.class);
        verify(registroRepository, never()).save(any());
    }

    private void entoncesSeDevuelvenLasEncontradasPorId() {
        assertThat(referenciables).isSameAs(encontrados);
    }

    private void entoncesSeDevuelveLaFilaEncontrada() {
        assertThat(referenciables).extracting(Registro::getId).containsExactly(REGISTRO_ID);
    }

    // --- datos ---

    private List<Campo> camposConPrecio() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).obligatorio(true).build(),
                Campo.builder().id(PRECIO_ID).tipo(TipoCampo.MONEDA).clave("price").build());
    }

    private List<Campo> conexiones() {
        return List.of(
                Campo.builder().id(CLIENTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(GARANTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).build());
    }

    private List<Campo> camposDelInventario() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).obligatorio(true).build(),
                Campo.builder().id(CANTIDAD_ID).tipo(TipoCampo.NUMERO).obligatorio(false).build());
    }

    private Registro unRegistroConPrecio(Object precio) {
        return Registro.builder()
                .id(REGISTRO_ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .valores(Map.of(NOMBRE_ID.toString(), "Harina", PRECIO_ID.toString(), precio))
                .creadoEn(ANTES)
                .actualizadoEn(ANTES)
                .build();
    }

    private Registro unRegistro() {
        return Registro.builder()
                .id(REGISTRO_ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .valores(Map.of(NOMBRE_ID.toString(), "Harina", CANTIDAD_ID.toString(), 3))
                .creadoEn(ANTES)
                .actualizadoEn(ANTES)
                .build();
    }
}
