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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private static final RegistroId ANA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
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
    @DisplayName("Anota el cambio de precio cuando se edita el precio de un producto del inventario")
    void editarShouldRecordPriceChangeWhenInventoryPriceChanges() {
        givenBloque(TipoBloque.INVENTARIO);
        Registro registro = registroConPrecio(1000);

        registroDatosService.editar(registro, camposConPrecio(), Map.of(PRECIO_ID.toString(), 1200), AHORA);

        thenCambioGuardado(new BigDecimal("1000"), new BigDecimal("1200"));
    }

    @Test
    @DisplayName("Anota el primer precio sin precio anterior")
    void editarShouldRecordFirstPriceWithoutPreviousPrice() {
        givenBloque(TipoBloque.INVENTARIO);

        registroDatosService.editar(registro(), camposConPrecio(), Map.of(PRECIO_ID.toString(), 1200), AHORA);

        thenCambioGuardado(null, new BigDecimal("1200"));
    }

    @Test
    @DisplayName("No anota nada cuando el precio no cambia")
    void editarShouldNotRecordWhenPriceIsTheSame() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registroDatosService.editar(registroConPrecio(1000), camposConPrecio(), Map.of(PRECIO_ID.toString(), 1000),
                AHORA);

        verifyNoInteractions(historialPrecioRepository);
    }

    @Test
    @DisplayName("No anota nada cuando se borra el precio")
    void editarShouldNotRecordWhenPriceIsCleared() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registroDatosService.editar(registroConPrecio(1000), camposConPrecio(),
                Collections.singletonMap(PRECIO_ID.toString(), null), AHORA);

        verifyNoInteractions(historialPrecioRepository);
    }

    @Test
    @DisplayName("No anota el precio de una fila de ventas: sólo cuenta el del inventario")
    void editarShouldNotRecordPriceOfNonInventoryBlock() {
        givenBloque(TipoBloque.VENTAS);

        registroDatosService.editar(registroConPrecio(1000), camposConPrecio(), Map.of(PRECIO_ID.toString(), 1200),
                AHORA);

        verifyNoInteractions(historialPrecioRepository);
    }

    @Test
    @DisplayName("No anota nada cuando el bloque del registro ya no existe")
    void editarShouldNotRecordWhenBloqueIsMissing() {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        registroDatosService.editar(registroConPrecio(1000), camposConPrecio(), Map.of(PRECIO_ID.toString(), 1200),
                AHORA);

        verifyNoInteractions(historialPrecioRepository);
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
    private void givenFilasExistentes(Set<RegistroId> buscados, RegistroId... existentes) {
        List<Registro> filas = List.of(existentes).stream()
                .map(id -> Registro.builder().id(id).bloqueId(CLIENTES_ID).build())
                .toList();
        when(registroRepository.findAllByIdsAndBloque(buscados, CLIENTES_ID, NEGOCIO_ID)).thenReturn(filas);
    }

    private void givenBloque(TipoBloque tipo) {
        when(registroRepository.save(any(Registro.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(Bloque.builder().id(BLOQUE_ID).tipo(tipo).build()));
    }

    // --- assert ---
    private void thenCambioGuardado(BigDecimal anterior, BigDecimal nuevo) {
        ArgumentCaptor<CambioPrecio> captor = ArgumentCaptor.forClass(CambioPrecio.class);
        verify(historialPrecioRepository).save(captor.capture());
        CambioPrecio cambio = captor.getValue();
        assertThat(cambio.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(cambio.registroId()).isEqualTo(REGISTRO_ID);
        assertThat(cambio.anterior()).isEqualTo(anterior);
        assertThat(cambio.nuevo()).isEqualTo(nuevo);
        assertThat(cambio.cambiadoEn()).isEqualTo(AHORA);
    }

    // --- helpers ---
    private List<Campo> camposConPrecio() {
        return List.of(
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).obligatorio(true).build(),
                Campo.builder().id(PRECIO_ID).tipo(TipoCampo.MONEDA).clave("price").build());
    }

    private Registro registroConPrecio(Object precio) {
        return Registro.builder()
                .id(REGISTRO_ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .valores(Map.of(NOMBRE_ID.toString(), "Harina", PRECIO_ID.toString(), precio))
                .creadoEn(ANTES)
                .actualizadoEn(ANTES)
                .build();
    }

    private List<Campo> conexiones() {
        return List.of(
                Campo.builder().id(CLIENTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(GARANTE_ID).tipo(TipoCampo.RELACION).bloqueRelacionadoId(CLIENTES_ID).build(),
                Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).build());
    }

    private Map<String, Object> valoresCliente(RegistroId filaId) {
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
