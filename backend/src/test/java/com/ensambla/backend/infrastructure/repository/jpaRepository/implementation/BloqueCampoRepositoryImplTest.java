package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.BloqueRepositoryMapper;
import com.ensambla.backend.infrastructure.repository.mapper.CampoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueCampoRepositoryImplTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");

    @Mock
    private IBloqueJpaRepository bloqueJpa;

    @Mock
    private ICampoJpaRepository campoJpa;

    @Test
    @DisplayName("El bloque se busca por id dentro del negocio y se mapea")
    void bloqueFindByIdAndNegocioIdShouldMapResult() {
        BloqueRepositoryImpl repository = new BloqueRepositoryImpl(bloqueJpa, new BloqueRepositoryMapper());
        when(bloqueJpa.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(BloqueEntity.builder().id(BLOQUE_ID).nombre("Productos").build()));

        Optional<Bloque> result = repository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);

        assertThat(result).map(Bloque::getNombre).contains("Productos");
    }

    @Test
    @DisplayName("Guardar un bloque persiste la entidad y devuelve lo que guardó la base")
    void bloqueSaveShouldPersistAndMapSavedEntity() {
        BloqueRepositoryImpl repository = new BloqueRepositoryImpl(bloqueJpa, new BloqueRepositoryMapper());
        when(bloqueJpa.save(any())).thenReturn(BloqueEntity.builder().id(BLOQUE_ID).nombre("Clientes").build());

        Bloque result = repository.save(Bloque.builder().negocioId(NEGOCIO_ID).nombre("Clientes").build());

        assertThat(result.getId()).isEqualTo(BLOQUE_ID);
        assertThat(result.getNombre()).isEqualTo("Clientes");
    }

    @Test
    @DisplayName("Eliminar un bloque delega el borrado con el filtro de negocio")
    void bloqueDeleteByIdAndNegocioIdShouldDelegate() {
        BloqueRepositoryImpl repository = new BloqueRepositoryImpl(bloqueJpa, new BloqueRepositoryMapper());

        repository.deleteByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);

        verify(bloqueJpa).deleteByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Los campos del bloque se devuelven mapeados y en el orden de la consulta")
    void campoFindByBloqueIdAndNegocioIdShouldMapInOrder() {
        CampoRepositoryImpl repository = new CampoRepositoryImpl(campoJpa, new CampoRepositoryMapper());
        when(campoJpa.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(
                CampoEntity.builder().nombre("Primero").build(),
                CampoEntity.builder().nombre("Segundo").build()));

        List<Campo> result = repository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID);

        assertThat(result).extracting(Campo::getNombre).containsExactly("Primero", "Segundo");
    }
}
