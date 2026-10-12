package com.ensambla.backend.domain.service.registro;

import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import java.math.BigDecimal;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.enums.DireccionOrden;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroDatosServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID NOMBRE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID CANTIDAD_ID = UUID.fromString("c0000000-0000-4000-8000-000000000002");
    private static final UUID CLIENTES_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final UUID CLIENTE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000003");
    private static final UUID GARANTE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000004");
    private static final UUID ANA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID BETO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final OffsetDateTime ANTES = OffsetDateTime.parse("2026-10-09T12:00:00Z");

    @Mock
    private RegistroRepository registroRepository;

    @InjectMocks
    private RegistroDatosService registroDatosService;

    @Test
    @DisplayName("Crea el registro con los valores validados y lo guarda")
    void crearShouldSaveRegistroWithValidatedValues() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> valores = Map.of(NOMBRE_ID.toString(), "  Harina ", "ajeno", 1);

        Registro result = registroDatosService.crear(NEGOCIO_ID, BLOQUE_ID, campos(), valores, AHORA);

        ArgumentCaptor<Registro> guardado = ArgumentCaptor.forClass(Registro.class);
        verify(registroRepository).save(guardado.capture());
        assertThat(result).isSameAs(guardado.getValue());
        assertThat(guardado.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getValue().getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(guardado.getValue().getValores()).containsOnly(Map.entry(NOMBRE_ID.toString(), "Harina"));
        assertThat(guardado.getValue().getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Guarda de una vez los registros ya validados")
    void crearVariosShouldSaveAll() {
        List<Registro> registros = List.of(registro());
        when(registroRepository.saveAll(registros)).thenReturn(registros);

        List<Registro> result = registroDatosService.crearVarios(registros);

        assertThat(result).isSameAs(registros);
    }

    @Test
    @DisplayName("No guarda nada cuando los valores no son válidos")
    void crearShouldNotSaveWhenValuesAreInvalid() {
        Map<String, Object> valores = Map.of(CANTIDAD_ID.toString(), 3);

        assertThatThrownBy(() -> registroDatosService.crear(NEGOCIO_ID, BLOQUE_ID, campos(), valores, AHORA))
                .isInstanceOf(RegistroInvalidoException.class);

        verifyNoMoreInteractions(registroRepository);
    }

    @Test
    @DisplayName("Devuelve el registro del negocio")
    void obtenerShouldReturnRegistroOfNegocio() {
        Registro registro = registro();
        when(registroRepository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.of(registro));

        Registro result = registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID);

        assertThat(result).isSameAs(registro);
    }

    @Test
    @DisplayName("Responde 'no existe' cuando el registro es de otro negocio o no existe")
    void obtenerShouldThrowWhenRegistroIsNotFound() {
        when(registroRepository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registroDatosService.obtener(REGISTRO_ID, NEGOCIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El registro no existe");
    }

    @Test
    @DisplayName("Lista los registros que entrega el repositorio para la consulta")
    void listarShouldReturnPaginaFromRepository() {
        ConsultaRegistros consulta = new ConsultaRegistros(
                NEGOCIO_ID, BLOQUE_ID, null, null, DireccionOrden.DESC, 0, 20);
        Pagina<Registro> pagina = new Pagina<>(List.of(registro()), 0, 20, 1);
        when(registroRepository.buscar(consulta)).thenReturn(pagina);

        Pagina<Registro> result = registroDatosService.listar(consulta);

        assertThat(result).isSameAs(pagina);
    }

    @Test
    @DisplayName("Edita sólo lo enviado, conserva el resto y actualiza la fecha de modificación")
    void editarShouldMergeChangesAndSave() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Registro registro = registro();
        Map<String, Object> cambios = Map.of(CANTIDAD_ID.toString(), 9);

        Registro result = registroDatosService.editar(registro, campos(), cambios, AHORA);

        assertThat(result.getValores())
                .containsEntry(NOMBRE_ID.toString(), "Harina")
                .containsEntry(CANTIDAD_ID.toString(), 9);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(result.getCreadoEn()).isEqualTo(ANTES);
    }

    @Test
    @DisplayName("No guarda la edición cuando un valor nuevo no es válido")
    void editarShouldNotSaveWhenChangesAreInvalid() {
        Registro registro = registro();
        Map<String, Object> cambios = Map.of(CANTIDAD_ID.toString(), "muchos");

        assertThatThrownBy(() -> registroDatosService.editar(registro, campos(), cambios, AHORA))
                .isInstanceOf(RegistroInvalidoException.class);

        verifyNoMoreInteractions(registroRepository);
        assertThat(registro.getValores()).containsEntry(CANTIDAD_ID.toString(), 3);
    }

    @Test
    @DisplayName("Elimina el registro por su id")
    void eliminarShouldDeleteById() {
        registroDatosService.eliminar(registro());

        verify(registroRepository).deleteById(REGISTRO_ID);
    }

    @Test
    @DisplayName("Totaliza la cantidad y la suma de cada campo numérico de los registros de la consulta")
    void totalesShouldSumNumericCamposOfTheQuery() {
        ConsultaRegistros consulta = new ConsultaRegistros(
                NEGOCIO_ID, BLOQUE_ID, "harina", null, DireccionOrden.DESC, 0, 20);
        when(registroRepository.sumarNumericos(consulta, List.of(CANTIDAD_ID)))
                .thenReturn(Map.of(CANTIDAD_ID, new BigDecimal("12.5")));

        TotalesRegistros result = registroDatosService.totales(consulta, campos(), 7);

        assertThat(result.cantidad()).isEqualTo(7);
        assertThat(result.sumas()).containsOnly(entry(CANTIDAD_ID, new BigDecimal("12.5")));
    }

    @Test
    @DisplayName("Sin campos numéricos devuelve sólo la cantidad")
    void totalesShouldReturnOnlyCountWhenThereAreNoNumericCampos() {
        ConsultaRegistros consulta = new ConsultaRegistros(
                NEGOCIO_ID, BLOQUE_ID, null, null, DireccionOrden.DESC, 0, 20);
        List<Campo> soloTexto = List.of(Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).build());
        when(registroRepository.sumarNumericos(consulta, List.of())).thenReturn(Map.of());

        TotalesRegistros result = registroDatosService.totales(consulta, soloTexto, 3);

        assertThat(result.cantidad()).isEqualTo(3);
        assertThat(result.sumas()).isEmpty();
    }

    @Test
    @DisplayName("Crea el registro cuando la fila a la que apunta la conexión existe")
    void crearShouldSaveWhenReferencedRowExists() {
        givenFilasExistentes(Set.of(ANA_ID), ANA_ID);
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Registro result = registroDatosService.crear(
                NEGOCIO_ID, BLOQUE_ID, conexiones(), valoresCliente(ANA_ID), AHORA);

        assertThat(result.getValores()).containsEntry(CLIENTE_ID.toString(), ANA_ID.toString());
    }

    @Test
    @DisplayName("Rechaza el registro cuando la fila a la que apunta la conexión ya no existe")
    void crearShouldRejectWhenReferencedRowIsMissing() {
        givenFilasExistentes(Set.of(ANA_ID));
        Map<String, Object> valores = valoresCliente(ANA_ID);

        assertThatThrownBy(() -> registroDatosService.crear(NEGOCIO_ID, BLOQUE_ID, conexiones(), valores, AHORA))
                .isInstanceOf(RegistroInvalidoException.class)
                .extracting(error -> ((RegistroInvalidoException) error).getErrores())
                .isEqualTo(Map.of(CLIENTE_ID.toString(), "Esa fila ya no existe"));
        verify(registroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Busca de una vez las filas de todas las conexiones a la misma tabla y marca sólo las que faltan")
    void crearShouldQueryOncePerBloqueAndReportOnlyMissingRows() {
        givenFilasExistentes(Set.of(ANA_ID, BETO_ID), ANA_ID);
        Map<String, Object> valores = Map.of(CLIENTE_ID.toString(), ANA_ID.toString(), GARANTE_ID.toString(),
                BETO_ID.toString());

        assertThatThrownBy(() -> registroDatosService.crear(NEGOCIO_ID, BLOQUE_ID, conexiones(), valores, AHORA))
                .isInstanceOf(RegistroInvalidoException.class)
                .extracting(error -> ((RegistroInvalidoException) error).getErrores())
                .isEqualTo(Map.of(GARANTE_ID.toString(), "Esa fila ya no existe"));
    }

    @Test
    @DisplayName("No consulta otras tablas cuando las conexiones quedan vacías")
    void crearShouldNotQueryWhenConexionesAreEmpty() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registroDatosService.crear(NEGOCIO_ID, BLOQUE_ID, conexiones(), Map.of(), AHORA);

        verify(registroRepository, never()).findAllByIdsAndBloque(any(), any(), any());
    }

    @Test
    @DisplayName("No guarda la edición cuando la conexión apunta a una fila que ya no existe")
    void editarShouldRejectWhenReferencedRowIsMissing() {
        givenFilasExistentes(Set.of(BETO_ID));
        Registro registro = registro();
        Map<String, Object> cambios = valoresCliente(BETO_ID);

        assertThatThrownBy(() -> registroDatosService.editar(registro, conexiones(), cambios, AHORA))
                .isInstanceOf(RegistroInvalidoException.class);
        verify(registroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Las referencias pedidas por id salen del repositorio filtradas por bloque y negocio")
    void buscarReferenciablesShouldFindByIdsWhenIdsArePresent() {
        List<Registro> encontrados = List.of(registro());
        when(registroRepository.findAllByIdsAndBloque(List.of(REGISTRO_ID), BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(encontrados);

        List<Registro> result = registroDatosService.buscarReferenciables(
                NEGOCIO_ID, BLOQUE_ID, campos(), "ignorado", List.of(REGISTRO_ID));

        assertThat(result).isSameAs(encontrados);
    }

    @Test
    @DisplayName("Sin ids busca por texto las 20 filas más nuevas del bloque")
    void buscarReferenciablesShouldSearchByTextoWhenIdsAreEmpty() {
        ConsultaRegistros consulta = new ConsultaRegistros(
                NEGOCIO_ID, BLOQUE_ID, "ana", null, DireccionOrden.DESC, 0, 20);
        when(registroRepository.buscar(consulta)).thenReturn(new Pagina<>(List.of(registro()), 0, 20, 1));

        List<Registro> result = registroDatosService.buscarReferenciables(
                NEGOCIO_ID, BLOQUE_ID, campos(), " ana ", List.of());

        assertThat(result).extracting(Registro::getId).containsExactly(REGISTRO_ID);
    }

    // --- arrange ---
    private void givenFilasExistentes(Set<UUID> buscados, UUID... existentes) {
        List<Registro> filas = List.of(existentes).stream()
                .map(id -> Registro.builder().id(id).bloqueId(CLIENTES_ID).build())
                .toList();
        when(registroRepository.findAllByIdsAndBloque(buscados, CLIENTES_ID, NEGOCIO_ID)).thenReturn(filas);
    }

    // --- helpers ---
    private List<Campo> conexiones() {
        return List.of(
                Campo.builder().id(CLIENTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(GARANTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).build());
    }

    private Map<String, Object> valoresCliente(UUID filaId) {
        return Map.of(CLIENTE_ID.toString(), filaId.toString());
    }

    private List<Campo> campos() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).obligatorio(true).build(),
                Campo.builder().id(CANTIDAD_ID).tipo(TipoCampo.NUMERO).obligatorio(false).build());
    }

    private Registro registro() {
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
