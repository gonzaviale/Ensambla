package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.ProgresoObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProgresoObjetivoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ProgresoObjetivoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgresoObjetivoRepositoryImplTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final BigDecimal VALOR = new BigDecimal("12600.00");

    private final IProgresoObjetivoJpaRepository jpa = mock(IProgresoObjetivoJpaRepository.class);
    private final ProgresoObjetivoRepositoryImpl repository =
            new ProgresoObjetivoRepositoryImpl(jpa, new ProgresoObjetivoRepositoryMapper());

    private static final UUID OTRA_FOTO_ID = UUID.fromString("00000000-0000-4000-8000-0000000000e2");

    private ProgresoObjetivo foto;
    private List<ProgresoObjetivo> fotos;

    @Test
    @DisplayName("Registra la foto de un mini-objetivo pasando al JPA los UUID y los valores")
    void registraLaFotoDeUnMiniObjetivoPasandoAlJpaLosUuidYLosValores() {
        dadoUnaFotoDeUnMiniObjetivo();

        cuandoSeRegistraLaFoto();

        entoncesSePasaronAlJpaLosUuidYLosValores(MINI_ID.valor());
    }

    @Test
    @DisplayName("Registra la foto del objetivo mayor con el mini-objetivo vacío")
    void registraLaFotoDelObjetivoMayorConElMiniObjetivoVacio() {
        dadoUnaFotoDelObjetivoMayor();

        cuandoSeRegistraLaFoto();

        entoncesSePasaronAlJpaLosUuidYLosValores(null);
    }

    @Test
    @DisplayName("Lista las fotos del objetivo en el orden de la consulta y las mapea al dominio")
    void listaLasFotosDelObjetivoEnElOrdenDeLaConsultaYLasMapeaAlDominio() {
        dadoQueElObjetivoTieneUnaFotoMayorYUnaDeMiniObjetivo();

        cuandoSeListanLasFotosDelObjetivo();

        entoncesSeListanLasDosFotosEnOrden();
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el objetivo no tiene fotos")
    void devuelveUnaListaVaciaCuandoElObjetivoNoTieneFotos() {
        when(jpa.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(OBJETIVO_ID.valor())).thenReturn(List.of());

        cuandoSeListanLasFotosDelObjetivo();

        entoncesNoSeListaNingunaFoto();
    }

    // --- dado ---
    private void dadoUnaFotoDeUnMiniObjetivo() {
        foto = ProgresoObjetivo.de(OBJETIVO_ID, MINI_ID, DIA, VALOR, AHORA);
    }

    private void dadoUnaFotoDelObjetivoMayor() {
        foto = ProgresoObjetivo.de(OBJETIVO_ID, null, DIA, VALOR, AHORA);
    }

    private void dadoQueElObjetivoTieneUnaFotoMayorYUnaDeMiniObjetivo() {
        when(jpa.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(OBJETIVO_ID.valor()))
                .thenReturn(List.of(entity(UUID.randomUUID(), null), entity(OTRA_FOTO_ID, MINI_ID.valor())));
    }

    private static ProgresoObjetivoEntity entity(UUID id, UUID miniObjetivoId) {
        return ProgresoObjetivoEntity.builder()
                .id(id)
                .objetivoId(OBJETIVO_ID.valor())
                .miniObjetivoId(miniObjetivoId)
                .dia(DIA)
                .valor(VALOR)
                .medidoEn(AHORA)
                .build();
    }

    // --- cuando ---
    private void cuandoSeRegistraLaFoto() {
        repository.registrar(foto);
    }

    private void cuandoSeListanLasFotosDelObjetivo() {
        fotos = repository.findAllByObjetivoId(OBJETIVO_ID);
    }

    // --- entonces ---
    private void entoncesSePasaronAlJpaLosUuidYLosValores(UUID miniObjetivoId) {
        verify(jpa).registrar(foto.id().valor(), OBJETIVO_ID.valor(), miniObjetivoId, DIA, VALOR, AHORA);
    }

    private void entoncesSeListanLasDosFotosEnOrden() {
        assertThat(fotos).hasSize(2);
        assertThat(fotos.get(0).esDelObjetivo()).isTrue();
        assertThat(fotos.get(1).id().valor()).isEqualTo(OTRA_FOTO_ID);
        assertThat(fotos.get(1).miniObjetivoId()).isEqualTo(MINI_ID);
    }

    private void entoncesNoSeListaNingunaFoto() {
        assertThat(fotos).isEmpty();
    }
}
