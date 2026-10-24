package com.ensambla.backend.domain.mapper.registro;

import com.ensambla.backend.domain.dto.registro.ReferenciaItem;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ListarReferenciasMapperTest {

    private static final UUID NOMBRE_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID ANA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID BETO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final List<Campo> CAMPOS = List.of(
            Campo.builder().id(NOMBRE_ID).tipo(TipoCampo.TEXTO).posicion((short) 0).build());

    private ListarReferenciasMapper listarReferenciasMapper;

    @BeforeEach
    void setUp() {
        listarReferenciasMapper = new ListarReferenciasMapper();
    }

    @Test
    @DisplayName("Arma el id y la etiqueta de cada registro conservando el orden")
    void toResponseShouldMapIdAndEtiquetaInOrder() {
        List<ReferenciaItem> result = listarReferenciasMapper.toResponse(
                List.of(registro(ANA_ID, "Ana"), registro(BETO_ID, "Beto")), CAMPOS);

        assertThat(result).containsExactly(new ReferenciaItem(ANA_ID, "Ana"), new ReferenciaItem(BETO_ID, "Beto"));
    }

    @Test
    @DisplayName("Sin registros devuelve una lista vacía")
    void toResponseShouldReturnEmptyListWhenThereAreNoRegistros() {
        List<ReferenciaItem> result = listarReferenciasMapper.toResponse(List.of(), CAMPOS);

        assertThat(result).isEmpty();
    }

    // --- helpers ---
    private Registro registro(UUID id, String nombre) {
        return Registro.builder().id(id).valores(Map.of(NOMBRE_ID.toString(), nombre)).build();
    }
}
