package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.infrastructure.repository.entity.PruebaPremiumEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class PruebaPremiumRepositoryMapperTest {

    private final PruebaPremiumRepositoryMapper pruebaPremiumRepositoryMapper = new PruebaPremiumRepositoryMapper();

    @Test
    @DisplayName("Convierte todos los campos de la entidad al dominio")
    void toDomainShouldMapEveryField() {
        PruebaPremiumEntity entity = PruebaPremiumEntity.builder()
                .negocioId(NEGOCIO_ID.valor())
                .inicioEn(INSTANTE)
                .finEn(INSTANTE.plusDays(7))
                .paginasIa(4)
                .build();

        PruebaPremium result = pruebaPremiumRepositoryMapper.toDomain(entity);

        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.inicioEn()).isEqualTo(INSTANTE);
        assertThat(result.finEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.paginasIa()).isEqualTo(4);
    }

    @Test
    @DisplayName("Convierte todos los campos del dominio a la entidad")
    void toEntityShouldMapEveryField() {
        PruebaPremium prueba = new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(7), 5);

        PruebaPremiumEntity result = pruebaPremiumRepositoryMapper.toEntity(prueba);

        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getInicioEn()).isEqualTo(INSTANTE);
        assertThat(result.getFinEn()).isEqualTo(INSTANTE.plusDays(7));
        assertThat(result.getPaginasIa()).isEqualTo(5);
    }
}
