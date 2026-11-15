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

    @Test
    @DisplayName("Registra la foto de un mini-objetivo pasando al JPA los UUID y los valores")
    void registrarShouldPassTheUuidsAndValues() {
        ProgresoObjetivo foto = ProgresoObjetivo.de(OBJETIVO_ID, MINI_ID, DIA, VALOR, AHORA);

        repository.registrar(foto);

        verify(jpa).registrar(foto.id().valor(), OBJETIVO_ID.valor(), MINI_ID.valor(), DIA, VALOR, AHORA);
    }

    @Test
    @DisplayName("Registra la foto del objetivo mayor con el mini-objetivo vacío")
    void registrarShouldPassNullForTheObjetivoPhoto() {
        ProgresoObjetivo foto = ProgresoObjetivo.de(OBJETIVO_ID, null, DIA, VALOR, AHORA);

        repository.registrar(foto);

        verify(jpa).registrar(foto.id().valor(), OBJETIVO_ID.valor(), null, DIA, VALOR, AHORA);
    }

    @Test
    @DisplayName("Lista las fotos del objetivo en el orden de la consulta y las mapea al dominio")
    void findAllByObjetivoIdShouldMapInOrder() {
        UUID otraFoto = UUID.fromString("00000000-0000-4000-8000-0000000000e2");
        when(jpa.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(OBJETIVO_ID.valor()))
                .thenReturn(List.of(entity(UUID.randomUUID(), null), entity(otraFoto, MINI_ID.valor())));

        List<ProgresoObjetivo> result = repository.findAllByObjetivoId(OBJETIVO_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).esDelObjetivo()).isTrue();
        assertThat(result.get(1).id().valor()).isEqualTo(otraFoto);
        assertThat(result.get(1).miniObjetivoId()).isEqualTo(MINI_ID);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el objetivo no tiene fotos")
    void findAllByObjetivoIdShouldReturnEmpty() {
        when(jpa.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(OBJETIVO_ID.valor())).thenReturn(List.of());

        List<ProgresoObjetivo> result = repository.findAllByObjetivoId(OBJETIVO_ID);

        assertThat(result).isEmpty();
    }

    // --- helpers ---
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
}
