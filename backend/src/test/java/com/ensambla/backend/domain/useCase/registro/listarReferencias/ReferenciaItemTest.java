package com.ensambla.backend.domain.useCase.registro.listarReferencias;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenciaItemTest {

    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final RegistroId ANA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId BETO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final List<Campo> CAMPOS = List.of(
            Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build());

    @Test
    @DisplayName("Arma el id y la etiqueta de cada registro conservando el orden")
    void deShouldMapIdAndEtiquetaInOrder() {
        List<ReferenciaItem> result = ReferenciaItem.de(
                List.of(registro(ANA_ID.valor(), "Ana"), registro(BETO_ID.valor(), "Beto")), CAMPOS);

        assertThat(result).containsExactly(new ReferenciaItem(ANA_ID, "Ana"), new ReferenciaItem(BETO_ID, "Beto"));
    }

    @Test
    @DisplayName("Sin registros devuelve una lista vacía")
    void deShouldReturnEmptyListWhenThereAreNoRegistros() {
        List<ReferenciaItem> result = ReferenciaItem.de(List.of(), CAMPOS);

        assertThat(result).isEmpty();
    }

    // --- helpers ---
    private Registro registro(UUID id, String nombre) {
        return Registro.builder().id(RegistroId.de(id)).valores(Map.of(NOMBRE_ID.toString(), nombre)).build();
    }
}
