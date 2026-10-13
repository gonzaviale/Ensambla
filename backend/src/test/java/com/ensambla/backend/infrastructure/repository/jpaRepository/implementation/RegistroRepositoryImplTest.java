package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import java.time.Instant;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import java.math.BigDecimal;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.enums.DireccionOrden;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IRegistroJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.RegistroRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroRepositoryImplTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");

    @Mock
    private IRegistroJpaRepository jpa;

    private RegistroRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RegistroRepositoryImpl(jpa, new RegistroRepositoryMapper());
    }

    @Test
    @DisplayName("Guarda el registro y devuelve el modelo persistido")
    void saveShouldPersistAndReturnDomain() {
        when(jpa.save(any(RegistroEntity.class))).thenReturn(entity());

        Registro result = repository.save(Registro.builder().bloqueId(BLOQUE_ID).valores(Map.of("k", "v")).build());

        assertThat(result.getId()).isEqualTo(REGISTRO_ID);
        assertThat(result.getValores()).containsEntry("k", "v");
    }

    @Test
    @DisplayName("Guarda varios registros juntos y devuelve los modelos persistidos")
    void saveAllShouldPersistAllAndReturnDomain() {
        when(jpa.saveAll(anyList())).thenReturn(List.of(entity()));

        List<Registro> result = repository.saveAll(List.of(Registro.builder().bloqueId(BLOQUE_ID).build()));

        assertThat(result).extracting(Registro::getId).containsExactly(REGISTRO_ID);
    }

    @Test
    @DisplayName("Busca por id dentro del negocio y mapea el resultado")
    void findByIdAndNegocioIdShouldMapResult() {
        when(jpa.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.of(entity()));

        Optional<Registro> result = repository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID);

        assertThat(result).map(Registro::getId).contains(REGISTRO_ID);
    }

    @Test
    @DisplayName("Devuelve vacío cuando el registro no es del negocio")
    void findByIdAndNegocioIdShouldReturnEmptyWhenMissing() {
        when(jpa.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThat(repository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Elimina por id")
    void deleteByIdShouldDelegateToJpa() {
        repository.deleteById(REGISTRO_ID);

        verify(jpa).deleteById(REGISTRO_ID);
    }

    @Test
    @DisplayName("Sin texto ni campo consulta en descendente sin patrón y devuelve la página mapeada")
    void buscarShouldQueryDescendingWithoutPatternAndMapPage() {
        Pageable pageable = PageRequest.of(1, 10);
        when(jpa.buscarDescendente(eq(NEGOCIO_ID), eq(BLOQUE_ID), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity()), pageable, 11));

        Pagina<Registro> result = repository.buscar(consulta(null, null, DireccionOrden.DESC, 1, 10));

        assertThat(result.contenido()).extracting(Registro::getId).containsExactly(REGISTRO_ID);
        assertThat(result.numero()).isEqualTo(1);
        assertThat(result.tamanio()).isEqualTo(10);
        assertThat(result.totalElementos()).isEqualTo(11);
    }

    @Test
    @DisplayName("En ascendente envía el patrón con los comodines escapados y el campo de orden")
    void buscarShouldQueryAscendingWithEscapedPatternAndCampo() {
        Pageable pageable = PageRequest.of(0, 20);
        when(jpa.buscarAscendente(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        repository.buscar(consulta("50%_a\\b", CAMPO_ID, DireccionOrden.ASC, 0, 20));

        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> campoOrden = ArgumentCaptor.forClass(String.class);
        verify(jpa).buscarAscendente(eq(NEGOCIO_ID), eq(BLOQUE_ID), patron.capture(), campoOrden.capture(),
                eq(pageable));
        assertThat(patron.getValue()).isEqualTo("%50\\%\\_a\\\\b%");
        assertThat(campoOrden.getValue()).isEqualTo(CAMPO_ID.toString());
    }

    @Test
    @DisplayName("Suma con el mismo patrón escapado del listado y devuelve las sumas por campo")
    void sumarNumericosShouldQueryWithEscapedPatternAndMapSums() {
        when(jpa.sumarNumericos(eq(NEGOCIO_ID), eq(BLOQUE_ID), eq("%50\\%%"), eq(List.of(CAMPO_ID.toString()))))
                .thenReturn(List.of(suma(CAMPO_ID, "1500.50")));

        Map<UUID, BigDecimal> result = repository.sumarNumericos(
                consulta("50%", null, DireccionOrden.DESC, 0, 20), List.of(CAMPO_ID));

        assertThat(result).containsOnly(entry(CAMPO_ID, new BigDecimal("1500.50")));
    }

    @Test
    @DisplayName("Sin campos numéricos no consulta la base")
    void sumarNumericosShouldNotQueryWhenThereAreNoCampos() {
        Map<UUID, BigDecimal> result = repository.sumarNumericos(
                consulta(null, null, DireccionOrden.DESC, 0, 20), List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(jpa);
    }

    @Test
    @DisplayName("Busca los registros pedidos por id dentro del bloque y del negocio")
    void findAllByIdsAndBloqueShouldQueryAndMap() {
        when(jpa.findAllByIdInAndBloqueIdAndNegocioIdOrderByCreadoEnDescIdDesc(
                List.of(REGISTRO_ID), BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(entity()));

        List<Registro> result = repository.findAllByIdsAndBloque(List.of(REGISTRO_ID), BLOQUE_ID, NEGOCIO_ID);

        assertThat(result).extracting(Registro::getId).containsExactly(REGISTRO_ID);
    }

    @Test
    @DisplayName("Sin ids no consulta la base")
    void findAllByIdsAndBloqueShouldNotQueryWhenIdsAreEmpty() {
        List<Registro> result = repository.findAllByIdsAndBloque(List.of(), BLOQUE_ID, NEGOCIO_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(jpa);
    }

    // --- helpers ---
    private IRegistroJpaRepository.SumaPorCampo suma(UUID campoId, String valor) {
        return new IRegistroJpaRepository.SumaPorCampo() {
            @Override
            public String getCampo() {
                return campoId.toString();
            }

            @Override
            public BigDecimal getSuma() {
                return new BigDecimal(valor);
            }
        };
    }

    private ConsultaRegistros consulta(String texto, UUID campoOrdenId, DireccionOrden direccion, int pagina,
                                       int tamanio) {
        return new ConsultaRegistros(NEGOCIO_ID, BLOQUE_ID, texto, campoOrdenId, direccion, pagina, tamanio);
    }

    private RegistroEntity entity() {
        return RegistroEntity.builder()
                .id(REGISTRO_ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .valores(Map.of("k", "v"))
                .build();
    }

    @Test
    @DisplayName("El reporte busca con el patrón escapado, las fechas y el filtro como texto")
    void buscarFiltradosShouldPassFiltersAsText() {
        Instant desde = Instant.parse("2026-10-01T03:00:00Z");
        Instant hasta = Instant.parse("2026-11-01T03:00:00Z");
        FiltroReporte filtro = new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, "50%", desde, hasta, CAMPO_ID, "true");
        when(jpa.buscarParaReporte(NEGOCIO_ID, BLOQUE_ID, "%50\\%%", desde.toString(), hasta.toString(),
                CAMPO_ID.toString(), "true", 10)).thenReturn(List.of(entity()));

        List<Registro> result = repository.buscarFiltrados(filtro, 10);

        assertThat(result).extracting(Registro::getId).containsExactly(REGISTRO_ID);
    }

    @Test
    @DisplayName("El reporte sin filtros pasa todo nulo")
    void buscarFiltradosShouldPassNullsWithoutFilters() {
        FiltroReporte sinFiltros = new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, null, null, null, null, null);
        when(jpa.buscarParaReporte(NEGOCIO_ID, BLOQUE_ID, null, null, null, null, null, 5)).thenReturn(List.of());

        assertThat(repository.buscarFiltrados(sinFiltros, 5)).isEmpty();
    }

    @Test
    @DisplayName("Cuenta las filas del reporte con el mismo filtro")
    void contarFiltradosShouldCountWithTheSameFilter() {
        FiltroReporte filtro = new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, null, null, null, CAMPO_ID, "x");
        when(jpa.contarParaReporte(NEGOCIO_ID, BLOQUE_ID, null, null, null, CAMPO_ID.toString(), "x")).thenReturn(7L);

        assertThat(repository.contarFiltrados(filtro)).isEqualTo(7L);
    }

    @Test
    @DisplayName("Suma las columnas numéricas del reporte y no consulta si no hay ninguna")
    void sumarNumericosOfReporteShouldMapSumsAndSkipWithoutColumns() {
        FiltroReporte filtro = new FiltroReporte(NEGOCIO_ID, BLOQUE_ID, "a", null, null, null, null);
        when(jpa.sumarNumericosParaReporte(NEGOCIO_ID, BLOQUE_ID, "%a%", null, null, null, null,
                List.of(CAMPO_ID.toString()))).thenReturn(List.of(suma(CAMPO_ID, "12.5")));

        Map<UUID, BigDecimal> result = repository.sumarNumericos(filtro, List.of(CAMPO_ID));

        assertThat(result).containsOnly(entry(CAMPO_ID, new BigDecimal("12.5")));
        assertThat(repository.sumarNumericos(filtro, List.of())).isEmpty();
    }
}
