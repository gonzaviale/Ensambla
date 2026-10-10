package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class NegocioRepositoryMapperTest {

    private final NegocioRepositoryMapper negocioRepositoryMapper = new NegocioRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos del negocio a la entidad")
    void toEntityShouldMapEveryField() {
        Negocio negocio = givenNegocio();

        NegocioEntity result = negocioRepositoryMapper.toEntity(negocio);

        assertThat(result).usingRecursiveComparison().isEqualTo(negocio);
    }

    @Test
    @DisplayName("Mapea todos los campos de la entidad al dominio")
    void toDomainShouldMapEveryField() {
        NegocioEntity entity = negocioRepositoryMapper.toEntity(givenNegocio());

        Negocio result = negocioRepositoryMapper.toDomain(entity);

        assertThat(result).usingRecursiveComparison().isEqualTo(entity);
    }

    @Test
    @DisplayName("Conserva campos nulos de una entidad nueva")
    void toDomainShouldKeepNullableFields() {
        NegocioEntity entity = new NegocioEntity();

        Negocio result = negocioRepositoryMapper.toDomain(entity);

        assertThat(result).usingRecursiveComparison().isEqualTo(entity);
    }

    // --- arrange ---
    private Negocio givenNegocio() {
        return Negocio.builder()
                .id(USUARIO_ID)
                .nombre("Mi negocio")
                .duenioId(USUARIO_ID)
                .creadoEn(FECHA)
                .actualizadoEn(FECHA)
                .build();
    }
}

