package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.ProgresoObjetivoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ProgresoObjetivoRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-0000000000e1");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final BigDecimal VALOR = new BigDecimal("12600.00");

    private final ProgresoObjetivoRepositoryMapper mapper = new ProgresoObjetivoRepositoryMapper();

    @Test
    @DisplayName("Mapea la foto de un mini-objetivo con todos sus campos")
    void unaFotoDeUnMiniObjetivoSeLeeConTodosSusDatos() {
        ProgresoObjetivoEntity entidad = dadoUnaFotoDeUnMiniObjetivo();

        ProgresoObjetivo progreso = cuandoSeConvierteEnProgreso(entidad);

        entoncesElProgresoTieneTodosLosDatosDeLaFoto(progreso);
    }

    @Test
    @DisplayName("La foto del objetivo mayor se mapea sin mini-objetivo")
    void laFotoDelObjetivoMayorSeLeeSinMiniObjetivo() {
        ProgresoObjetivoEntity entidad = dadoUnaFotoDelObjetivoMayor();

        ProgresoObjetivo progreso = cuandoSeConvierteEnProgreso(entidad);

        entoncesElProgresoEsDelObjetivoYNoDeUnMini(progreso);
    }

    // --- dado ---
    private ProgresoObjetivoEntity dadoUnaFotoDeUnMiniObjetivo() {
        return unaFotoDe(MINI_ID.valor());
    }

    private ProgresoObjetivoEntity dadoUnaFotoDelObjetivoMayor() {
        return unaFotoDe(null);
    }

    private ProgresoObjetivoEntity unaFotoDe(UUID miniObjetivoId) {
        return ProgresoObjetivoEntity.builder()
                .id(ID)
                .objetivoId(OBJETIVO_ID.valor())
                .miniObjetivoId(miniObjetivoId)
                .dia(DIA)
                .valor(VALOR)
                .medidoEn(AHORA)
                .build();
    }

    // --- cuando ---
    private ProgresoObjetivo cuandoSeConvierteEnProgreso(ProgresoObjetivoEntity entidad) {
        return mapper.toDomain(entidad);
    }

    // --- entonces ---
    private void entoncesElProgresoTieneTodosLosDatosDeLaFoto(ProgresoObjetivo progreso) {
        assertThat(progreso.id().valor()).isEqualTo(ID);
        assertThat(progreso.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(progreso.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(progreso.dia()).isEqualTo(DIA);
        assertThat(progreso.valor()).isEqualTo(VALOR);
        assertThat(progreso.medidoEn()).isEqualTo(AHORA);
    }

    private void entoncesElProgresoEsDelObjetivoYNoDeUnMini(ProgresoObjetivo progreso) {
        assertThat(progreso.miniObjetivoId()).isNull();
        assertThat(progreso.esDelObjetivo()).isTrue();
    }
}
