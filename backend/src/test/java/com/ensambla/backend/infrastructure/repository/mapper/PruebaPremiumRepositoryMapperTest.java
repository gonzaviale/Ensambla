package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.infrastructure.repository.entity.PruebaPremiumEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.PlanTestData.INSTANTE;
import static com.ensambla.backend.support.PlanTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class PruebaPremiumRepositoryMapperTest {

    private static final int DIAS_DE_PRUEBA = 7;
    private static final int PAGINAS_DE_LA_ENTIDAD = 4;
    private static final int PAGINAS_DEL_DOMINIO = 5;

    private final PruebaPremiumRepositoryMapper pruebaPremiumRepositoryMapper = new PruebaPremiumRepositoryMapper();

    @Test
    @DisplayName("Convierte todos los campos de la entidad al dominio")
    void unaEntidadSeLeeComoPruebaPremiumConTodosSusDatos() {
        PruebaPremiumEntity entidad = dadoUnaEntidadDePruebaPremium();

        PruebaPremium prueba = cuandoSeConvierteEnPrueba(entidad);

        entoncesLaPruebaTieneTodosLosDatosDeLaEntidad(prueba);
    }

    @Test
    @DisplayName("Convierte todos los campos del dominio a la entidad")
    void unaPruebaPremiumSeGuardaConTodosSusDatos() {
        PruebaPremium prueba = dadoUnaPruebaPremium();

        PruebaPremiumEntity entidad = cuandoSeConvierteEnEntidad(prueba);

        entoncesLaEntidadTieneTodosLosDatosDeLaPrueba(entidad);
    }

    // --- dado ---
    private PruebaPremiumEntity dadoUnaEntidadDePruebaPremium() {
        return PruebaPremiumEntity.builder()
                .negocioId(NEGOCIO_ID.valor())
                .inicioEn(INSTANTE)
                .finEn(INSTANTE.plusDays(DIAS_DE_PRUEBA))
                .paginasIa(PAGINAS_DE_LA_ENTIDAD)
                .build();
    }

    private PruebaPremium dadoUnaPruebaPremium() {
        return new PruebaPremium(NEGOCIO_ID, INSTANTE, INSTANTE.plusDays(DIAS_DE_PRUEBA), PAGINAS_DEL_DOMINIO);
    }

    // --- cuando ---
    private PruebaPremium cuandoSeConvierteEnPrueba(PruebaPremiumEntity entidad) {
        return pruebaPremiumRepositoryMapper.toDomain(entidad);
    }

    private PruebaPremiumEntity cuandoSeConvierteEnEntidad(PruebaPremium prueba) {
        return pruebaPremiumRepositoryMapper.toEntity(prueba);
    }

    // --- entonces ---
    private void entoncesLaPruebaTieneTodosLosDatosDeLaEntidad(PruebaPremium prueba) {
        assertThat(prueba.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(prueba.inicioEn()).isEqualTo(INSTANTE);
        assertThat(prueba.finEn()).isEqualTo(INSTANTE.plusDays(DIAS_DE_PRUEBA));
        assertThat(prueba.paginasIa()).isEqualTo(PAGINAS_DE_LA_ENTIDAD);
    }

    private void entoncesLaEntidadTieneTodosLosDatosDeLaPrueba(PruebaPremiumEntity entidad) {
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getInicioEn()).isEqualTo(INSTANTE);
        assertThat(entidad.getFinEn()).isEqualTo(INSTANTE.plusDays(DIAS_DE_PRUEBA));
        assertThat(entidad.getPaginasIa()).isEqualTo(PAGINAS_DEL_DOMINIO);
    }
}
