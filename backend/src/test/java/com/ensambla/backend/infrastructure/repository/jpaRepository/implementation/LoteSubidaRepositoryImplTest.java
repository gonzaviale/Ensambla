package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IFotoCuadernoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ILoteSubidaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.LoteSubidaRepositoryMapper;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoteSubidaRepositoryImplTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("00000000-0000-4000-8000-000000000015");

    private final ILoteSubidaJpaRepository loteSubidaJpaRepository = mock(ILoteSubidaJpaRepository.class);
    private final IFotoCuadernoJpaRepository fotoCuadernoJpaRepository = mock(IFotoCuadernoJpaRepository.class);
    private final LoteSubidaRepositoryMapper loteSubidaRepositoryMapper = new LoteSubidaRepositoryMapper();
    private final LoteSubidaRepositoryImpl loteSubidaRepositoryImpl = new LoteSubidaRepositoryImpl(
            loteSubidaJpaRepository, fotoCuadernoJpaRepository, loteSubidaRepositoryMapper);

    @Test
    @DisplayName("Guarda el lote y después sus fotos apuntando al id que le dio la base")
    void saveConFotosShouldLinkFotosToSavedLote() {
        givenLoteSaved();
        givenFotosSaved();

        LoteSubida result = loteSubidaRepositoryImpl.saveConFotos(LoteTestData.lote(List.of(fotoSinLote())));

        assertThat(result.getId()).isEqualTo(LOTE_ID);
        assertThat(result.getFotos()).extracting("id").containsExactly(FOTO_ID);
        assertThat(thenSavedFotos()).extracting(FotoCuadernoEntity::getLoteId).containsExactly(LOTE_ID.valor());
    }

    @Test
    @DisplayName("Confirma el lote cuando la sentencia condicional cambió una fila")
    void confirmarShouldReturnTrueWhenOneRowChanged() {
        givenEstadoChanges(1);

        boolean result = loteSubidaRepositoryImpl.confirmar(LOTE_ID, NEGOCIO_ID, BLOQUE_ID, AHORA);

        assertThat(result).isTrue();
        verify(loteSubidaJpaRepository)
                .cambiarEstado(LOTE_ID.valor(), NEGOCIO_ID.valor(), EstadoLote.LISTO, EstadoLote.CONFIRMADO,
                BLOQUE_ID.valor(), AHORA);
    }

    @Test
    @DisplayName("No confirma cuando el lote ya no estaba listo o es de otro negocio")
    void confirmarShouldReturnFalseWhenNoRowChanged() {
        givenEstadoChanges(0);

        boolean result = loteSubidaRepositoryImpl.confirmar(LOTE_ID, NEGOCIO_ID, BLOQUE_ID, AHORA);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Trae una foto del negocio por su id")
    void findFotoByIdAndNegocioIdShouldMapFoto() {
        when(fotoCuadernoJpaRepository.findByIdAndNegocioId(FOTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(loteSubidaRepositoryMapper.toEntity(LoteTestData.foto())));

        Optional<FotoCuaderno> result = loteSubidaRepositoryImpl.findFotoByIdAndNegocioId(FOTO_ID, NEGOCIO_ID);

        assertThat(result).map(FotoCuaderno::getId).contains(FOTO_ID);
    }

    @Test
    @DisplayName("Una foto de otro negocio no se encuentra")
    void findFotoByIdAndNegocioIdShouldReturnEmptyWhenFotoIsForeign() {
        when(fotoCuadernoJpaRepository.findByIdAndNegocioId(FOTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.empty());

        assertThat(loteSubidaRepositoryImpl.findFotoByIdAndNegocioId(FOTO_ID, NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Trae el lote del negocio con sus fotos ordenadas")
    void findByIdAndNegocioIdShouldLoadFotos() {
        givenLoteFound();
        givenFotosFound();

        Optional<LoteSubida> result = loteSubidaRepositoryImpl.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getFotos()).extracting("id").containsExactly(FOTO_ID);
    }

    @Test
    @DisplayName("No busca fotos cuando el lote no es del negocio")
    void findByIdAndNegocioIdShouldReturnEmptyWhenLoteMissing() {
        givenLoteMissing();

        Optional<LoteSubida> result = loteSubidaRepositoryImpl.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(fotoCuadernoJpaRepository);
    }

    // --- arrange ---
    private void givenLoteSaved() {
        when(loteSubidaJpaRepository.saveAndFlush(any())).thenReturn(loteEntity());
    }

    private void givenFotosSaved() {
        when(fotoCuadernoJpaRepository.saveAllAndFlush(anyList()))
                .thenReturn(List.of(loteSubidaRepositoryMapper.toEntity(LoteTestData.foto())));
    }

    private void givenEstadoChanges(int filas) {
        when(loteSubidaJpaRepository.cambiarEstado(any(), any(), any(), any(), any(), any())).thenReturn(filas);
    }

    private void givenLoteFound() {
        when(loteSubidaJpaRepository.findByIdAndNegocioId(LOTE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(loteEntity()));
    }

    private void givenFotosFound() {
        when(fotoCuadernoJpaRepository
                .findAllByLoteIdAndNegocioIdOrderByPosicionAsc(LOTE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(List.of(loteSubidaRepositoryMapper.toEntity(LoteTestData.foto())));
    }

    private void givenLoteMissing() {
        when(loteSubidaJpaRepository.findByIdAndNegocioId(LOTE_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.empty());
    }

    // --- assert ---
    @SuppressWarnings("unchecked")
    private List<FotoCuadernoEntity> thenSavedFotos() {
        ArgumentCaptor<List<FotoCuadernoEntity>> fotos = ArgumentCaptor.forClass(List.class);
        verify(fotoCuadernoJpaRepository).saveAllAndFlush(fotos.capture());
        return fotos.getValue();
    }

    // --- helpers ---
    private LoteSubidaEntity loteEntity() {
        return loteSubidaRepositoryMapper.toEntity(LoteTestData.lote(List.of()));
    }

    private FotoCuaderno fotoSinLote() {
        FotoCuaderno foto = LoteTestData.foto();
        foto.setId(null);
        foto.setLoteId(null);
        return foto;
    }
}
