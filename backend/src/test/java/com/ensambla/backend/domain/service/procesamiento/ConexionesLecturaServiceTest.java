package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConexionesLecturaServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID VENTAS_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CLIENTES_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID CAMPO_CLIENTE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID CAMPO_MONTO_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID CAMPO_NOMBRE_ID = UUID.fromString("40000000-0000-0000-0000-000000000003");
    private static final UUID JUAN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OTRO_JUAN_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final ConsumoIa CONSUMO = new ConsumoIa("simulado", "demo", "v2", 1, 10, 20, 30);

    @Mock
    private CampoService campoService;

    @Mock
    private RegistroDatosService registroDatosService;

    @InjectMocks
    private ConexionesLecturaService conexionesLecturaService;

    @Test
    @DisplayName("Reemplaza el texto leído por el id de la única fila de la tabla conectada que se llama igual")
    void resolverShouldReplaceTextWithRowId() {
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
    void resolverShouldKeepNewFields() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));
        PropuestaLectura base = propuesta(fila(1, dato("juan perez", 0.8)));
        CampoPropuesto telefono = new CampoPropuesto("telefono", "Teléfono", TipoCampo.TEXTO, List.of());

        PropuestaLectura result = resolver(new PropuestaLectura(base.lectura(), base.mapeoCampos(), List.of(telefono)));

        assertThat(result.camposNuevos()).containsExactly(telefono);
    }

    @Test
    @DisplayName("Si no hay ninguna fila con ese nombre deja la celda vacía, sin confianza, y avisa")
    void resolverShouldLeaveEmptyWhenNothingMatches() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));

        PropuestaLectura result = resolver(propuesta(fila(1, dato("Mariana", 0.9))));

        assertThat(result.lectura().filas().getFirst().datos().get("cliente")).isEqualTo(new DatoLeido(null, 0.0));
        assertThat(result.lectura().advertencias()).containsExactly(
                "Aviso", "No encontramos «Mariana» en «Clientes», o hay más de una igual: elegila vos.");
    }

    @Test
    @DisplayName("Si hay más de una fila con ese nombre no adivina: deja la celda vacía y avisa")
    void resolverShouldLeaveEmptyWhenSeveralMatch() {
        givenClientes(registro(JUAN_ID, "Juan"), registro(OTRO_JUAN_ID, "juan"));

        PropuestaLectura result = resolver(propuesta(fila(1, dato("Juan", 0.9))));

        assertThat(result.lectura().filas().getFirst().datos().get("cliente").valor()).isNull();
        assertThat(result.lectura().advertencias()).hasSize(2);
    }

    @Test
    @DisplayName("No cuenta como coincidencia una fila que sólo contiene el texto buscado")
    void resolverShouldRequireTheWholeNameToMatch() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));

        PropuestaLectura result = resolver(propuesta(fila(1, dato("Juan", 0.9))));

        assertThat(result.lectura().filas().getFirst().datos().get("cliente").valor()).isNull();
    }

    @Test
    @DisplayName("Consulta una sola vez cada nombre repetido y avisa una sola vez")
    void resolverShouldLookUpEachDistinctTextOnce() {
        givenClientes(registro(JUAN_ID, "Juan Pérez"));
        PropuestaLectura propuesta = propuesta(
                fila(1, dato("Juan Pérez", 0.9)), fila(2, dato("JUAN PEREZ", 0.7)),
                fila(3, dato("Mariana", 0.9)), fila(4, dato("mariana", 0.9)));

        PropuestaLectura result = resolver(propuesta);

        assertThat(result.lectura().filas()).extracting(fila -> fila.datos().get("cliente").valor())
                .containsExactly(JUAN_ID.toString(), JUAN_ID.toString(), null, null);
        assertThat(result.lectura().advertencias()).hasSize(2);
        verify(campoService, times(1)).listarDeBloque(CLIENTES_ID, NEGOCIO_ID);
        verify(registroDatosService, times(2)).buscarReferenciables(
                eq(NEGOCIO_ID), eq(CLIENTES_ID), any(), any(), eq(List.of()));
    }

    @Test
    @DisplayName("Deja como están las celdas de conexión vacías o en blanco")
    void resolverShouldIgnoreBlankValues() {
        PropuestaLectura propuesta = propuesta(fila(1, new DatoLeido(null, 0.9)), fila(2, dato("  ", 0.9)));

        PropuestaLectura result = resolver(propuesta);

        assertThat(result.lectura().filas()).isEqualTo(propuesta.lectura().filas());
        verifyNoInteractions(registroDatosService);
    }

    @Test
    @DisplayName("Una fila que no cargó la columna de conexión queda igual")
    void resolverShouldIgnoreRowsWithoutTheColumn() {
        FilaLeida sinCliente = new FilaLeida(1, 1, Map.of("monto", new DatoLeido("100", 0.9)));

        PropuestaLectura result = resolver(propuesta(sinCliente));

        assertThat(result.lectura().filas()).containsExactly(sinCliente);
    }

    @Test
    @DisplayName("Sin columnas de conexión devuelve la misma propuesta sin consultar nada")
    void resolverShouldReturnSamePropuestaWithoutConexiones() {
        LecturaCuaderno lectura = lectura(null, List.of(new CampoPropuesto("monto", "Monto", TipoCampo.NUMERO, List.of())),
                List.of(fila(1, dato("Juan", 0.9))));
        PropuestaLectura propuesta = new PropuestaLectura(lectura, Map.of());

        PropuestaLectura result = resolver(propuesta);

        assertThat(result).isSameAs(propuesta);
        verifyNoInteractions(campoService, registroDatosService);
    }

    @Test
    @DisplayName("Una columna de conexión sin campo mapeado en el bloque queda como la leyó la IA")
    void resolverShouldIgnoreConexionWithoutMappedCampo() {
        PropuestaLectura propuesta = new PropuestaLectura(
                lectura(VENTAS_ID, campos(), List.of(fila(1, dato("Juan", 0.9)))), Map.of());

        assertThat(resolver(propuesta)).isSameAs(propuesta);
        verifyNoInteractions(campoService, registroDatosService);
    }

    @Test
    @DisplayName("Un campo mapeado que no es una conexión no se resuelve")
    void resolverShouldIgnoreMappedCampoThatIsNotAConexion() {
        PropuestaLectura propuesta = new PropuestaLectura(
                lectura(VENTAS_ID, campos(), List.of(fila(1, dato("Juan", 0.9)))),
                Map.of("cliente", CAMPO_MONTO_ID));

        assertThat(resolver(propuesta)).isSameAs(propuesta);
        verifyNoInteractions(campoService, registroDatosService);
    }

    @Test
    @DisplayName("Una propuesta para un bloque nuevo no tiene conexiones que resolver")
    void resolverShouldIgnorePropuestaForNewBloque() {
        PropuestaLectura propuesta = new PropuestaLectura(
                lectura(null, campos(), List.of(fila(1, dato("Juan", 0.9)))), Map.of());

        assertThat(resolver(propuesta)).isSameAs(propuesta);
        verifyNoInteractions(campoService, registroDatosService);
    }

    // --- arrange ---
    private void givenClientes(Registro... registros) {
        Campo nombre = Campo.builder().id(CAMPO_NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build();
        when(campoService.listarDeBloque(CLIENTES_ID, NEGOCIO_ID)).thenReturn(List.of(nombre));
        when(registroDatosService.buscarReferenciables(eq(NEGOCIO_ID), eq(CLIENTES_ID), any(), any(), eq(List.of())))
                .thenReturn(List.of(registros));
    }

    private Registro registro(UUID id, String nombre) {
        return Registro.builder().id(id).valores(Map.of(CAMPO_NOMBRE_ID.toString(), nombre)).build();
    }

    private PropuestaLectura propuesta(FilaLeida... filas) {
        return new PropuestaLectura(lectura(VENTAS_ID, campos(), List.of(filas)),
                Map.of("cliente", CAMPO_CLIENTE_ID, "monto", CAMPO_MONTO_ID));
    }

    private List<CampoPropuesto> campos() {
        return List.of(new CampoPropuesto("cliente", "Cliente", TipoCampo.RELACION, List.of()),
                new CampoPropuesto("monto", "Monto", TipoCampo.NUMERO, List.of()));
    }

    private LecturaCuaderno lectura(UUID bloqueId, List<CampoPropuesto> campos, List<FilaLeida> filas) {
        return new LecturaCuaderno(TipoRegistroCuaderno.VENTAS, new BloquePropuesto("Ventas", bloqueId, campos),
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
    private PropuestaLectura resolver(PropuestaLectura propuesta) {
        return conexionesLecturaService.resolver(NEGOCIO_ID, propuesta, bloques());
    }
}
