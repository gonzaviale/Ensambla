package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import com.ensambla.backend.domain.model.marca.Tipografia;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.KitDeMarcaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KitDeMarcaRepositoryMapperTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-15T10:00:00Z");
    private static final byte[] IMAGEN = {1, 2, 3};

    private final KitDeMarcaRepositoryMapper mapper = new KitDeMarcaRepositoryMapper();

    private KitDeMarca kit;

    private KitDeMarcaEntity entity;

    @Test
    @DisplayName("Mapea la entidad con logo al dominio con todos sus campos")
    void toDomainShouldMapEveryFieldWithLogo() {
        givenUnaEntidadConLogo();

        whenSeMapeaAlDominio();

        assertThat(kit.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(kit.getColorPrincipal().valor()).isEqualTo("#CC0000");
        assertThat(kit.getColorSecundario().valor()).isEqualTo("#00CC00");
        assertThat(kit.getTipografia()).isEqualTo(Tipografia.NUNITO);
        assertThat(kit.getTono()).isEqualTo(TonoDeMarca.DIVERTIDO);
        assertThat(kit.getLogo().contenido()).isEqualTo(IMAGEN);
        assertThat(kit.getLogo().tipoMime()).isEqualTo("image/png");
        assertThat(kit.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mapea la entidad sin logo al dominio sin logo")
    void toDomainShouldMapMissingLogo() {
        givenUnaEntidadSinLogo();

        whenSeMapeaAlDominio();

        assertThat(kit.tieneLogo()).isFalse();
    }

    @Test
    @DisplayName("Mapea el dominio con logo a la entidad con todos sus campos")
    void toEntityShouldMapEveryFieldWithLogo() {
        givenUnKitConLogo();

        whenSeMapeaALaEntidad();

        assertThat(entity.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entity.getColorPrincipal()).isEqualTo("#00506A");
        assertThat(entity.getLogo()).isEqualTo(IMAGEN);
        assertThat(entity.getLogoTipoMime()).isEqualTo("image/png");
        assertThat(entity.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mapea el dominio sin logo a la entidad sin logo ni tipo")
    void toEntityShouldMapMissingLogo() {
        givenUnKitSinLogo();

        whenSeMapeaALaEntidad();

        assertThat(entity.getLogo()).isNull();
        assertThat(entity.getLogoTipoMime()).isNull();
    }

    private KitDeMarcaEntity.KitDeMarcaEntityBuilder entidadBase() {
        return KitDeMarcaEntity.builder()
                .id(UUID.randomUUID())
                .negocioId(NEGOCIO_ID.valor())
                .colorPrincipal("#CC0000")
                .colorSecundario("#00CC00")
                .tipografia(Tipografia.NUNITO)
                .tono(TonoDeMarca.DIVERTIDO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA);
    }

    private void givenUnaEntidadConLogo() {
        entity = entidadBase().logo(IMAGEN).logoTipoMime("image/png").build();
    }

    private void givenUnaEntidadSinLogo() {
        entity = entidadBase().build();
    }

    private void givenUnKitSinLogo() {
        kit = KitDeMarca.predeterminado(NEGOCIO_ID, AHORA);
    }

    private void givenUnKitConLogo() {
        givenUnKitSinLogo();
        kit.ponerLogo(new LogoDeMarca(IMAGEN, "image/png"), AHORA);
    }

    private void whenSeMapeaAlDominio() {
        kit = mapper.toDomain(entity);
    }

    private void whenSeMapeaALaEntidad() {
        entity = mapper.toEntity(kit);
    }
}
