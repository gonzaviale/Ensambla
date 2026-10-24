package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IConsultasPorNegocioJpaRepositoryTest extends PostgresDatosTestSupport {

    @Test
    @DisplayName("Encuentra el negocio por el id de su dueño")
    void findFirstByDuenioIdShouldReturnNegocioOfTheOwner() {
        NegocioEntity negocio = givenNegocio("dueña@example.com");

        Optional<NegocioEntity> result = negocios.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(negocio.getDuenioId());

        assertThat(result).map(NegocioEntity::getId).contains(negocio.getId());
    }

    @Test
    @DisplayName("No devuelve negocio para un usuario que no es dueño de ninguno")
    void findFirstByDuenioIdShouldReturnEmptyWhenUsuarioOwnsNothing() {
        UUID usuarioSinNegocio = givenUsuario("sin-negocio@example.com");

        assertThat(negocios.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(usuarioSinNegocio)).isEmpty();
    }

    @Test
    @DisplayName("Con más de un negocio devuelve el más antiguo")
    void findFirstByDuenioIdShouldReturnOldestWhenOwnerHasSeveral() {
        NegocioEntity primero = givenNegocio("dueña@example.com");
        negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Segundo negocio")
                .duenioId(primero.getDuenioId())
                .creadoEn(AHORA.plusDays(1))
                .actualizadoEn(AHORA.plusDays(1))
                .build());

        Optional<NegocioEntity> result = negocios.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(primero.getDuenioId());

        assertThat(result).map(NegocioEntity::getId).contains(primero.getId());
    }

    @Test
    @DisplayName("Encuentra el bloque sólo dentro de su negocio")
    void findByIdAndNegocioIdShouldRespectNegocioForBloques() {
        UUID negocioA = givenNegocio("a@example.com").getId();
        UUID negocioB = givenNegocio("b@example.com").getId();
        UUID bloqueId = givenBloque(NegocioId.de(negocioA), "Productos");

        Optional<BloqueEntity> propio = bloques.findByIdAndNegocioId(bloqueId, negocioA);
        Optional<BloqueEntity> ajeno = bloques.findByIdAndNegocioId(bloqueId, negocioB);

        assertThat(propio).isPresent();
        assertThat(ajeno).isEmpty();
    }

    @Test
    @DisplayName("Lista los campos del bloque por posición y no mezcla los de otro negocio")
    void findByBloqueIdAndNegocioIdShouldListCamposByPosicionScopedToNegocio() {
        UUID negocioA = givenNegocio("a@example.com").getId();
        UUID negocioB = givenNegocio("b@example.com").getId();
        UUID bloqueId = givenBloque(NegocioId.de(negocioA), "Productos");
        givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueId), "Segundo", TipoCampo.NUMERO, 1);
        givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueId), "Primero", TipoCampo.TEXTO, 0);

        List<CampoEntity> propios = campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioA);
        List<CampoEntity> ajenos = campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioB);

        assertThat(propios).extracting(CampoEntity::getNombre).containsExactly("Primero", "Segundo");
        assertThat(ajenos).isEmpty();
    }
}
