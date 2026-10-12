package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.CampoRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampoRepositoryImplTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    @Mock
    private ICampoJpaRepository campoJpa;

    private CampoRepositoryImpl campoRepositoryImpl;

    @BeforeEach
    void setUp() {
        campoRepositoryImpl = new CampoRepositoryImpl(campoJpa, new CampoRepositoryMapper());
    }

    @Test
    @DisplayName("El campo se busca por id dentro del negocio y se mapea")
    void findByIdAndNegocioIdShouldMapResult() {
        when(campoJpa.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(CampoEntity.builder().id(CAMPO_ID).nombre("Precio").build()));

        Optional<Campo> result = campoRepositoryImpl.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID);

        assertThat(result).map(Campo::getNombre).contains("Precio");
    }

    @Test
    @DisplayName("Un campo de otro negocio o inexistente vuelve vacío")
    void findByIdAndNegocioIdShouldReturnEmptyWhenMissing() {
        when(campoJpa.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        Optional<Campo> result = campoRepositoryImpl.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Guardar un campo persiste la entidad completa y devuelve lo que guardó la base")
    void saveShouldPersistAndMapSavedEntity() {
        ArgumentCaptor<CampoEntity> guardada = ArgumentCaptor.forClass(CampoEntity.class);
        when(campoJpa.save(guardada.capture()))
                .thenReturn(CampoEntity.builder().id(CAMPO_ID).nombre("Talle").tipo(TipoCampo.OPCION).build());

        Campo result = campoRepositoryImpl.save(givenCampoNuevo());

        assertThat(result.getId()).isEqualTo(CAMPO_ID);
        assertThat(result.getTipo()).isEqualTo(TipoCampo.OPCION);
        assertThat(guardada.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardada.getValue().getOpciones()).containsExactly("Chico", "Grande");
    }

    @Test
    @DisplayName("Las conexiones hacia un bloque se buscan dentro del negocio y se mapean")
    void findRelacionesHaciaShouldMapResult() {
        when(campoJpa.findRelacionesHacia(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(CampoEntity.builder().id(CAMPO_ID).nombre("Cliente").tipo(TipoCampo.RELACION)
                        .bloqueRelacionadoId(BLOQUE_ID).build()));

        List<Campo> result = campoRepositoryImpl.findRelacionesHacia(BLOQUE_ID, NEGOCIO_ID);

        assertThat(result).extracting(Campo::getNombre).containsExactly("Cliente");
        assertThat(result).extracting(Campo::getBloqueRelacionadoId).containsExactly(BLOQUE_ID);
    }

    @Test
    @DisplayName("Eliminar un campo delega el borrado con el filtro de negocio")
    void deleteByIdAndNegocioIdShouldDelegate() {
        campoRepositoryImpl.deleteByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID);

        verify(campoJpa).deleteByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID);
    }

    // --- helpers ---
    private Campo givenCampoNuevo() {
        return Campo.builder()
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre("Talle")
                .tipo(TipoCampo.OPCION)
                .opciones(List.of("Chico", "Grande"))
                .build();
    }
}
