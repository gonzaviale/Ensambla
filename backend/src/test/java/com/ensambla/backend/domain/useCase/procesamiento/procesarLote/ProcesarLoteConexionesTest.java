package com.ensambla.backend.domain.useCase.procesamiento.procesarLote;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.FotoLectura;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.ReclamoId;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/** Cómo el worker convierte el texto leído en columnas de conexión en la fila a la que apunta. */
class ProcesarLoteConexionesTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("10000000-0000-0000-0000-000000000001");
    private static final BloqueId VENTAS_ID = BloqueId.de("30000000-0000-0000-0000-000000000001");
    private static final BloqueId CLIENTES_ID = BloqueId.de("30000000-0000-0000-0000-000000000002");
    private static final CampoId CAMPO_CLIENTE_ID = CampoId.de("40000000-0000-0000-0000-000000000001");
    private static final CampoId CAMPO_MONTO_ID = CampoId.de("40000000-0000-0000-0000-000000000002");
    private static final CampoId CAMPO_NOMBRE_ID = CampoId.de("40000000-0000-0000-0000-000000000003");
    private static final UUID JUAN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OTRO_JUAN_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final ConsumoIa CONSUMO = new ConsumoIa("simulado", "demo", "v2", 1, 10, 20, 30);
    private static final LoteTrabajo LOTE = new LoteTrabajo(LoteId.nuevo(), NEGOCIO_ID, null, ReclamoId.nuevo(), 1);
    private static final FotoLectura FOTO = new FotoLectura("foto.jpg", "image/jpeg");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroDatosService registroDatosService;

    @Mock
    private ProcesamientoRepository procesamientoRepository;

    @Mock
    private ArchivoCuadernoPort archivoCuadernoPort;

    @Mock
    private LectorCuadernoPort lectorCuadernoPort;


    @Mock
    private ControlPlanService controlPlanService;

    private ProcesarLote procesarLote;

    @BeforeEach
    void setUp() {
        procesarLote = new ProcesarLote(procesamientoRepository, archivoCuadernoPort, lectorCuadernoPort,
                campoRepository, registroDatosService, controlPlanService,
                ProcesamientoTestData.politica(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Reemplaza el texto leído por el id de la única fila de la tabla conectada que se llama igual")
    void executeShouldReplaceTextWithRowId() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));
        PropuestaLectura propuesta = propuesta(fila(1, dato("juan perez", 0.8)));

        PropuestaLectura result = resolver(propuesta);

        assertThat(result.lectura().filas().getFirst().datos().get("cliente"))
                .isEqualTo(new DatoLeido(JUAN_ID.toString(), 0.8));
        assertThat(result.lectura().filas().getFirst().datos().get("monto")).isEqualTo(new DatoLeido("100", 0.9));
        assertThat(result.lectura().advertencias()).containsExactly("Aviso");
        assertThat(result.mapeoCampos()).isEqualTo(propuesta.mapeoCampos());
    }

    @Test
    @DisplayName("Al resolver las conexiones conserva los campos nuevos detectados")
    void executeShouldKeepNewFields() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));
        CampoPropuesto telefono = new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of());
        FilaLeida fila = new FilaLeida(1, 1, Map.of("cliente", dato("juan perez", 0.8), "monto", dato("100", 0.9),
                "telefono", dato("1155", 0.9)));
        List<CampoPropuesto> columnas = List.of(campos().get(0), campos().get(1), telefono);

        PropuestaLectura result = resolver(lectura("Ventas", VENTAS_ID, columnas, List.of(fila)), VENTAS_ID, bloques());

        assertThat(result.camposNuevos()).containsExactly(telefono);
    }

    @Test
    @DisplayName("Si no hay ninguna fila con ese nombre deja la celda vacía, sin confianza, y avisa")
    void executeShouldLeaveEmptyWhenNothingMatches() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));

        PropuestaLectura result = resolver(propuesta(fila(1, dato("Mariana", 0.9))));

        assertThat(result.lectura().filas().getFirst().datos().get("cliente")).isEqualTo(new DatoLeido(null, 0.0));
        assertThat(result.lectura().advertencias()).containsExactly(
                "Aviso", "No encontramos «Mariana» en «Clientes», o hay más de una igual: elegila vos.");
    }

    @Test
    @DisplayName("Si hay más de una fila con ese nombre no adivina: deja la celda vacía y avisa")
    void executeShouldLeaveEmptyWhenSeveralMatch() {
        givenClientes(registro(JUAN_ID, "Juan"), registro(OTRO_JUAN_ID, "juan"));

        PropuestaLectura result = resolver(propuesta(fila(1, dato("Juan", 0.9))));

        assertThat(result.lectura().filas().getFirst().datos().get("cliente").valor()).isNull();
        assertThat(result.lectura().advertencias()).hasSize(2);
    }

    @Test
    @DisplayName("No cuenta como coincidencia una fila que sólo contiene el texto buscado")
    void executeShouldRequireTheWholeNameToMatch() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));

        PropuestaLectura result = resolver(propuesta(fila(1, dato("Juan", 0.9))));

        assertThat(result.lectura().filas().getFirst().datos().get("cliente").valor()).isNull();
    }

    @Test
    @DisplayName("Consulta una sola vez cada nombre repetido y avisa una sola vez")
    void executeShouldLookUpEachDistinctTextOnce() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));
        PropuestaLectura propuesta = propuesta(
                fila(1, dato("Juan Pérez", 0.9)), fila(2, dato("JUAN PEREZ", 0.7)),
                fila(3, dato("Mariana", 0.9)), fila(4, dato("mariana", 0.9)));

        PropuestaLectura result = resolver(propuesta);

        assertThat(result.lectura().filas()).extracting(fila -> fila.datos().get("cliente").valor())
                .containsExactly(JUAN_ID.toString(), JUAN_ID.toString(), null, null);
        assertThat(result.lectura().advertencias()).hasSize(2);
        verify(campoRepository, times(1)).findByBloqueIdAndNegocioId(CLIENTES_ID, NEGOCIO_ID);
        verify(registroDatosService, times(2)).buscarReferenciables(
                eq(NEGOCIO_ID), eq(CLIENTES_ID), any(), any(), eq(List.of()));
    }

    @Test
    @DisplayName("Deja como están las celdas de conexión vacías o en blanco")
    void executeShouldIgnoreBlankValues() {
        PropuestaLectura propuesta = propuesta(fila(1, new DatoLeido(null, 0.9)), fila(2, dato("  ", 0.9)));

        PropuestaLectura result = resolver(propuesta);

        assertThat(result.lectura().filas()).isEqualTo(propuesta.lectura().filas());
        verifyNoInteractions(registroDatosService);
    }

    @Test
    @DisplayName("Una fila que no cargó la columna de conexión queda igual")
    void executeShouldIgnoreRowsWithoutTheColumn() {
        FilaLeida sinCliente = new FilaLeida(1, 1, Map.of("monto", new DatoLeido("100", 0.9)));

        PropuestaLectura result = resolver(propuesta(sinCliente));

        assertThat(result.lectura().filas()).containsExactly(sinCliente);
    }

    @Test
    @DisplayName("Sin columnas de conexión devuelve la misma propuesta sin consultar nada")
    void executeShouldReturnSamePropuestaWithoutConexiones() {
        LecturaCuaderno lectura = lectura("Ventas", null,
                List.of(new CampoPropuesto("monto", "Monto", TipoCampo.NUMERO, List.of())),
                List.of(fila(1, dato("Juan", 0.9))));

        PropuestaLectura result = resolver(lectura, null, bloques());

        assertThat(result).isEqualTo(PropuestaLectura.proponer(lectura, bloques(), null));
        verifyNoInteractions(campoRepository, registroDatosService);

    }

    @Test
    @DisplayName("Una columna de conexión sin campo mapeado en el bloque queda como la leyó la IA")
    void executeShouldIgnoreConexionWithoutMappedCampo() {
        LecturaCuaderno lectura = lectura("Ventas", VENTAS_ID, campos(), List.of(fila(1, dato("Juan", 0.9))));
        List<BloqueLectura> sinCliente = List.of(new BloqueLectura(VENTAS_ID, "Ventas", List.of(
                new CampoConocido(CAMPO_MONTO_ID, "Monto", TipoCampo.NUMERO, List.of()))));

        PropuestaLectura result = resolver(lectura, VENTAS_ID, sinCliente);

        assertThat(result).isEqualTo(PropuestaLectura.proponer(lectura, sinCliente, VENTAS_ID));
        verifyNoInteractions(campoRepository, registroDatosService);

    }

    @Test
    @DisplayName("Un campo mapeado que no es una conexión no se resuelve")
    void executeShouldIgnoreMappedCampoThatIsNotAConexion() {
        LecturaCuaderno lectura = lectura("Ventas", VENTAS_ID,
                List.of(new CampoPropuesto("cliente", "Monto", TipoCampo.RELACION, List.of())),
                List.of(fila(1, dato("Juan", 0.9))));

        PropuestaLectura result = resolver(lectura, VENTAS_ID, bloques());

        assertThat(result).isEqualTo(PropuestaLectura.proponer(lectura, bloques(), VENTAS_ID));
        verifyNoInteractions(campoRepository, registroDatosService);

    }

    @Test
    @DisplayName("Una propuesta para un bloque nuevo no tiene conexiones que resolver")
    void executeShouldIgnorePropuestaForNewBloque() {
        LecturaCuaderno lectura = lectura("Otra tabla", null, campos(), List.of(fila(1, dato("Juan", 0.9))));

        PropuestaLectura result = resolver(lectura, null, bloques());

        assertThat(result).isEqualTo(PropuestaLectura.proponer(lectura, bloques(), null));
        assertThat(result.lectura().bloque().bloqueExistenteId()).isNull();
        verifyNoInteractions(campoRepository, registroDatosService);

    }

    // --- arrange ---
    private void givenClientes(Registro... registros) {
        Campo nombre = Campo.builder().id(CAMPO_NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build();
        when(campoRepository.findByBloqueIdAndNegocioId(CLIENTES_ID, NEGOCIO_ID)).thenReturn(List.of(nombre));
        when(registroDatosService.buscarReferenciables(eq(NEGOCIO_ID), eq(CLIENTES_ID), any(), any(), eq(List.of())))
                .thenReturn(List.of(registros));
    }

    private Registro registro(UUID id, String nombre) {
        return Registro.builder().id(RegistroId.de(id)).valores(Map.of(CAMPO_NOMBRE_ID.toString(), nombre)).build();
    }

    private PropuestaLectura propuesta(FilaLeida... filas) {
        return new PropuestaLectura(lectura("Ventas", VENTAS_ID, campos(), List.of(filas)),
                Map.of("cliente", CAMPO_CLIENTE_ID, "monto", CAMPO_MONTO_ID));
    }

    private List<CampoPropuesto> campos() {
        return List.of(new CampoPropuesto("cliente", "Cliente", TipoCampo.RELACION, List.of()),
                new CampoPropuesto("monto", "Monto", TipoCampo.NUMERO, List.of()));
    }

    private LecturaCuaderno lectura(String nombre, BloqueId bloqueId, List<CampoPropuesto> campos,
                                    List<FilaLeida> filas) {
        return new LecturaCuaderno(TipoRegistroCuaderno.VENTAS, new BloquePropuesto(nombre, bloqueId, campos),
                filas, List.of("Aviso"), CONSUMO);
    }

    private FilaLeida fila(int renglon, DatoLeido cliente) {
        return new FilaLeida(1, renglon, Map.of("cliente", cliente, "monto", new DatoLeido("100", 0.9)));
    }

    private DatoLeido dato(String valor, double confianza) {
        return new DatoLeido(valor, confianza);
    }

    private List<BloqueLectura> bloques() {
        return List.of(
                new BloqueLectura(VENTAS_ID, "Ventas", List.of(
                        new CampoConocido(CAMPO_CLIENTE_ID, "Cliente", TipoCampo.RELACION, List.of(), CLIENTES_ID),
                        new CampoConocido(CAMPO_MONTO_ID, "Monto", TipoCampo.NUMERO, List.of()))),
                new BloqueLectura(CLIENTES_ID, "Clientes", List.of()));
    }

    // --- act ---
    /** Procesa un lote cuya lectura propone {@code propuesta} y devuelve la propuesta con la que finalizó. */
    private PropuestaLectura resolver(PropuestaLectura propuesta) {
        return resolver(propuesta.lectura(), null, bloques());
    }

    private PropuestaLectura resolver(LecturaCuaderno lectura, BloqueId destino, List<BloqueLectura> bloques) {
        LoteTrabajo lote = new LoteTrabajo(LOTE.id(), NEGOCIO_ID, destino, LOTE.reclamoId(), 1);
        when(procesamientoRepository.reclamar(eq(AHORA), any())).thenReturn(Optional.of(lote));
        when(procesamientoRepository.fotos(lote)).thenReturn(List.of(FOTO));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, FOTO.clave())).thenReturn(new byte[]{1});
        when(procesamientoRepository.bloques(NEGOCIO_ID)).thenReturn(bloques);
        when(controlPlanService.reservarPaginasIa(eq(NEGOCIO_ID), eq(1), eq(AHORA), any())).thenReturn(true);
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura);

        procesarLote.execute();

        ArgumentCaptor<ResultadoIntento> resultado = ArgumentCaptor.forClass(ResultadoIntento.class);
        verify(procesamientoRepository).finalizar(eq(lote), resultado.capture(), any(), eq(AHORA), any());
        return resultado.getValue().propuesta();
    }
}
