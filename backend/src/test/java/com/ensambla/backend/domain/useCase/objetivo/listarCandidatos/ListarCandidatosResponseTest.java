package com.ensambla.backend.domain.useCase.objetivo.listarCandidatos;

import com.ensambla.backend.domain.model.objetivo.SentidoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse.CandidatoItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static org.assertj.core.api.Assertions.assertThat;

class ListarCandidatosResponseTest {

    @Test
    @DisplayName("Copia todos los datos de cada candidato, en el mismo orden, y le suma su sentido")
    void deShouldCopyEveryFieldInOrder() {
        ListarCandidatosResponse result = ListarCandidatosResponse.de(
                List.of(candidato(), candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "4000")));

        assertThat(result.content()).hasSize(2);
        CandidatoItem primero = result.content().getFirst();
        assertThat(primero.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(primero.sentido()).isEqualTo(SentidoMiniObjetivo.SUBE);
        assertThat(primero.titulo()).isEqualTo(TITULO);
        assertThat(primero.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(primero.referencia()).isEqualTo(PRODUCTO);
        assertThat(primero.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(primero.valorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(primero.meta()).isEqualTo(META_MINI);
        assertThat(primero.aporteEstimado()).isEqualTo(APORTE);
        assertThat(result.content().get(1).sentido()).isEqualTo(SentidoMiniObjetivo.BAJA);
    }

    @Test
    @DisplayName("Sin candidatos devuelve una lista vacía")
    void deShouldReturnAnEmptyListWhenThereAreNoCandidates() {
        assertThat(ListarCandidatosResponse.de(List.of()).content()).isEmpty();
    }
}
