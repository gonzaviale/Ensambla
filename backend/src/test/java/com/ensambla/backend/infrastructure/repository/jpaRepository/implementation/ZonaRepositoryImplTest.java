package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.infrastructure.repository.entity.ZonaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IZonaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ZonaRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZonaRepositoryImplTest {

    private final IZonaJpaRepository zonaJpaRepository = mock(IZonaJpaRepository.class);
    private final ZonaRepositoryImpl zonaRepository = new ZonaRepositoryImpl(
            zonaJpaRepository, new ZonaRepositoryMapper());

    private static final UUID ZONA_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");

    private List<Zona> zonas;
    private Optional<Zona> zona;

    @Test
    @DisplayName("Lista todas las zonas como dominio")
    void listaTodasLasZonasComoDominio() {
        dadoQueExisteLaZonaSanJusto();

        cuandoSeListanLasZonas();

        entoncesSeListaLaZonaSanJusto();
    }

    @Test
    @DisplayName("Busca una zona por id")
    void buscaUnaZonaPorId() {
        dadoQueExisteLaZonaSanJusto();

        cuandoSeBuscaLaZonaPorId();

        entoncesSeEncuentraLaZonaSanJusto();
    }

    // --- dado ---
    private void dadoQueExisteLaZonaSanJusto() {
        ZonaEntity entidad = ZonaEntity.builder().id(ZONA_ID).nombre("San Justo").build();
        when(zonaJpaRepository.findAll()).thenReturn(List.of(entidad));
        when(zonaJpaRepository.findById(ZONA_ID)).thenReturn(Optional.of(entidad));
    }

    // --- cuando ---
    private void cuandoSeListanLasZonas() {
        zonas = zonaRepository.findAll();
    }

    private void cuandoSeBuscaLaZonaPorId() {
        zona = zonaRepository.findById(ZonaId.de(ZONA_ID));
    }

    // --- entonces ---
    private void entoncesSeListaLaZonaSanJusto() {
        assertThat(zonas).extracting(Zona::getId).containsExactly(ZonaId.de(ZONA_ID));
    }

    private void entoncesSeEncuentraLaZonaSanJusto() {
        assertThat(zona).map(Zona::getNombre).contains("San Justo");
    }
}
