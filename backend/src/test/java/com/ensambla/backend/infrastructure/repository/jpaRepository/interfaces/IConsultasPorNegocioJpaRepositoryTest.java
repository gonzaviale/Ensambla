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

    private NegocioEntity negocio;
    private Optional<NegocioEntity> negocioEncontrado;
    private Optional<String> emailEncontrado;
    private Optional<String> emailDeNegocioInexistente;
    private UUID usuarioSinNegocio;
    private UUID negocioA;
    private UUID negocioB;
    private UUID bloqueId;
    private Optional<BloqueEntity> bloquePropio;
    private Optional<BloqueEntity> bloqueAjeno;
    private List<CampoEntity> camposPropios;
    private List<CampoEntity> camposAjenos;

    @Test
    @DisplayName("Encuentra el negocio por el id de su dueño")
    void encuentraElNegocioPorElIdDeSuDuenio() {
        dadoUnNegocioConDuenia();

        cuandoSeBuscaElNegocioDelDuenio();

        entoncesSeEncuentraElNegocio();
    }

    @Test
    @DisplayName("Encuentra el email del dueño del negocio y nada si el negocio no existe")
    void encuentraElEmailDelDuenioDelNegocioYNadaSiElNegocioNoExiste() {
        dadoUnNegocioConDuenia();

        cuandoSeBuscaElEmailDelDuenioDelNegocioYDeUnoInexistente();

        entoncesSoloSeEncuentraElEmailDelNegocioExistente();
    }

    @Test
    @DisplayName("No devuelve negocio para un usuario que no es dueño de ninguno")
    void noDevuelveNegocioParaUnUsuarioQueNoEsDuenioDeNinguno() {
        dadoUnUsuarioSinNegocio();

        cuandoSeBuscaElNegocioDelUsuarioSinNegocio();

        entoncesNoSeEncuentraNingunNegocio();
    }

    @Test
    @DisplayName("Con más de un negocio devuelve el más antiguo")
    void conMasDeUnNegocioDevuelveElMasAntiguo() {
        dadoUnaDuenaConDosNegociosElSegundoMasNuevo();

        cuandoSeBuscaElNegocioDelDuenio();

        entoncesSeEncuentraElNegocio();
    }

    @Test
    @DisplayName("Encuentra el bloque sólo dentro de su negocio")
    void encuentraElBloqueSoloDentroDeSuNegocio() {
        dadoUnBloqueDelNegocioA();

        cuandoSeBuscaElBloqueEnSuNegocioYEnOtro();

        entoncesSoloSeEncuentraElBloqueEnSuNegocio();
    }

    @Test
    @DisplayName("Lista los campos del bloque por posición y no mezcla los de otro negocio")
    void listaLosCamposDelBloquePorPosicionYNoMezclaLosDeOtroNegocio() {
        dadoUnBloqueDelNegocioAConDosCamposDesordenados();

        cuandoSeListanLosCamposDelBloqueEnSuNegocioYEnOtro();

        entoncesLosCamposVienenPorPosicionYNoSeMezclanConOtroNegocio();
    }

    // --- dado ---
    private void dadoUnNegocioConDuenia() {
        negocio = givenNegocio("dueña@example.com");
    }

    private void dadoUnUsuarioSinNegocio() {
        usuarioSinNegocio = givenUsuario("sin-negocio@example.com");
    }

    private void dadoUnaDuenaConDosNegociosElSegundoMasNuevo() {
        negocio = givenNegocio("dueña@example.com");
        negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Segundo negocio")
                .duenioId(negocio.getDuenioId())
                .creadoEn(AHORA.plusDays(1))
                .actualizadoEn(AHORA.plusDays(1))
                .build());
    }

    private void dadoUnBloqueDelNegocioA() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueId = givenBloque(NegocioId.de(negocioA), "Productos");
    }

    private void dadoUnBloqueDelNegocioAConDosCamposDesordenados() {
        dadoUnBloqueDelNegocioA();
        givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueId), "Segundo", TipoCampo.NUMERO, 1);
        givenCampo(NegocioId.de(negocioA), BloqueId.de(bloqueId), "Primero", TipoCampo.TEXTO, 0);
    }

    // --- cuando ---
    private void cuandoSeBuscaElNegocioDelDuenio() {
        negocioEncontrado = negocios.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(negocio.getDuenioId());
    }

    private void cuandoSeBuscaElNegocioDelUsuarioSinNegocio() {
        negocioEncontrado = negocios.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(usuarioSinNegocio);
    }

    private void cuandoSeBuscaElEmailDelDuenioDelNegocioYDeUnoInexistente() {
        emailEncontrado = negocios.findEmailDuenioById(negocio.getId());
        emailDeNegocioInexistente = negocios.findEmailDuenioById(UUID.randomUUID());
    }

    private void cuandoSeBuscaElBloqueEnSuNegocioYEnOtro() {
        bloquePropio = bloques.findByIdAndNegocioId(bloqueId, negocioA);
        bloqueAjeno = bloques.findByIdAndNegocioId(bloqueId, negocioB);
    }

    private void cuandoSeListanLosCamposDelBloqueEnSuNegocioYEnOtro() {
        camposPropios = campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioA);
        camposAjenos = campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioB);
    }

    // --- entonces ---
    private void entoncesSeEncuentraElNegocio() {
        assertThat(negocioEncontrado).map(NegocioEntity::getId).contains(negocio.getId());
    }

    private void entoncesNoSeEncuentraNingunNegocio() {
        assertThat(negocioEncontrado).isEmpty();
    }

    private void entoncesSoloSeEncuentraElEmailDelNegocioExistente() {
        assertThat(emailEncontrado).contains("dueña@example.com");
        assertThat(emailDeNegocioInexistente).isEmpty();
    }

    private void entoncesSoloSeEncuentraElBloqueEnSuNegocio() {
        assertThat(bloquePropio).isPresent();
        assertThat(bloqueAjeno).isEmpty();
    }

    private void entoncesLosCamposVienenPorPosicionYNoSeMezclanConOtroNegocio() {
        assertThat(camposPropios).extracting(CampoEntity::getNombre).containsExactly("Primero", "Segundo");
        assertThat(camposAjenos).isEmpty();
    }
}
