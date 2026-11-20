package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.support.IdsDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class NegocioRepositoryMapperTest {

    private final NegocioRepositoryMapper negocioRepositoryMapper = new NegocioRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos del negocio a la entidad")
    void unNegocioSeGuardaConTodosSusDatos() {
        Negocio negocio = dadoUnNegocio();

        NegocioEntity entidad = cuandoSeConvierteEnEntidad(negocio);

        entoncesLaEntidadTieneLosMismosDatosQueElNegocio(entidad, negocio);
    }

    @Test
    @DisplayName("Mapea todos los campos de la entidad al dominio")
    void unaEntidadSeLeeComoNegocioConTodosSusDatos() {
        NegocioEntity entidad = dadoUnaEntidadDeNegocio();

        Negocio negocio = cuandoSeConvierteEnNegocio(entidad);

        entoncesElNegocioTieneLosMismosDatosQueLaEntidad(negocio, entidad);
    }

    @Test
    @DisplayName("Conserva campos nulos de una entidad nueva")
    void unaEntidadNuevaSeLeeConservandoLosCamposNulos() {
        NegocioEntity entidad = dadoUnaEntidadNueva();

        Negocio negocio = cuandoSeConvierteEnNegocio(entidad);

        entoncesElNegocioTieneLosMismosDatosQueLaEntidad(negocio, entidad);
    }

    // --- dado ---
    private Negocio dadoUnNegocio() {
        return Negocio.builder()
                .id(NegocioId.de(USUARIO_ID.valor()))
                .nombre("Mi negocio")
                .duenioId(USUARIO_ID)
                .cobroAlias("mi.alias.mp")
                .cobroTitular("Ana Pérez")
                .creadoEn(FECHA)
                .actualizadoEn(FECHA)
                .build();
    }

    private NegocioEntity dadoUnaEntidadDeNegocio() {
        return negocioRepositoryMapper.toEntity(dadoUnNegocio());
    }

    private NegocioEntity dadoUnaEntidadNueva() {
        return new NegocioEntity();
    }

    // --- cuando ---
    private NegocioEntity cuandoSeConvierteEnEntidad(Negocio negocio) {
        return negocioRepositoryMapper.toEntity(negocio);
    }

    private Negocio cuandoSeConvierteEnNegocio(NegocioEntity entidad) {
        return negocioRepositoryMapper.toDomain(entidad);
    }

    // --- entonces ---
    private void entoncesLaEntidadTieneLosMismosDatosQueElNegocio(NegocioEntity entidad, Negocio negocio) {
        assertThat(entidad).usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(IdsDePrueba::mismoValor, IdsDePrueba.CAMPOS_DE_ID)
                .isEqualTo(negocio);
    }

    private void entoncesElNegocioTieneLosMismosDatosQueLaEntidad(Negocio negocio, NegocioEntity entidad) {
        assertThat(negocio).usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(IdsDePrueba::mismoValor, IdsDePrueba.CAMPOS_DE_ID)
                .isEqualTo(entidad);
    }
}
