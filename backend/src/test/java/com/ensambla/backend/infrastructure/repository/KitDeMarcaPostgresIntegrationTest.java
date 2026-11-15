package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.model.marca.KitDeMarca;
import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.repository.marca.KitDeMarcaRepository;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class KitDeMarcaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-15T10:00:00Z");
    private static final byte[] IMAGEN = {1, 2, 3};

    @Autowired
    private KitDeMarcaRepository kitDeMarcaRepository;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    private NegocioId negocioPropio;

    private NegocioId negocioAjeno;

    private Optional<KitDeMarca> kitLeido;

    @Test
    @DisplayName("Guarda el kit con su logo y lo devuelve igual")
    void saveShouldStoreTheKitWithItsLogo() {
        givenUnNegocioConKitYLogo();

        whenSeLeeElKitDelNegocioPropio();

        assertThat(kitLeido).get().satisfies(kit -> {
            assertThat(kit.getColorPrincipal().valor()).isEqualTo("#00506A");
            assertThat(kit.getLogo().contenido()).isEqualTo(IMAGEN);
            assertThat(kit.getLogo().tipoMime()).isEqualTo("image/png");
        });
    }

    @Test
    @DisplayName("Un negocio no ve el kit de otro: la búsqueda por negocio ajeno viene vacía")
    void findByNegocioIdShouldNotSeeAnotherNegociosKit() {
        givenUnNegocioConKitYLogo();
        givenOtroNegocioSinKit();

        whenSeLeeElKitDelNegocioAjeno();

        assertThat(kitLeido).isEmpty();
    }

    private void givenUnNegocioConKitYLogo() {
        negocioPropio = givenNegocio();
        KitDeMarca kit = KitDeMarca.predeterminado(negocioPropio, AHORA);
        kit.ponerLogo(new LogoDeMarca(IMAGEN, "image/png"), AHORA);
        kitDeMarcaRepository.save(kit);
    }

    private void givenOtroNegocioSinKit() {
        negocioAjeno = givenNegocio();
    }

    private void whenSeLeeElKitDelNegocioPropio() {
        kitLeido = kitDeMarcaRepository.findByNegocioId(negocioPropio);
    }

    private void whenSeLeeElKitDelNegocioAjeno() {
        kitLeido = kitDeMarcaRepository.findByNegocioId(negocioAjeno);
    }

    private NegocioId givenNegocio() {
        UUID usuarioId = usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        UUID negocioId = negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        return NegocioId.de(negocioId);
    }
}
