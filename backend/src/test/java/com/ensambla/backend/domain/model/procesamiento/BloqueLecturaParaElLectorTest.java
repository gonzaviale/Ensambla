package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueLecturaParaElLectorTest {

    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId CLIENTES_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("Le muestra a la IA cada columna con su tipo y el nombre de la tabla a la que apunta")
    void paraElLectorShouldNameConnectedTables() {
        BloqueLectura clientes = new BloqueLectura(CLIENTES_ID, "Clientes", List.of());
        BloqueLectura ventas = new BloqueLectura(VENTAS_ID, "Ventas", List.of(
                new CampoConocido(CampoId.nuevo(), "Monto", TipoCampo.MONEDA, List.of()),
                new CampoConocido(CampoId.nuevo(), "Cliente", TipoCampo.RELACION, List.of(), CLIENTES_ID)));

        BloqueConocido conocido = ventas.paraElLector(List.of(ventas, clientes));

        assertThat(conocido.id()).isEqualTo(VENTAS_ID);
        assertThat(conocido.nombre()).isEqualTo("Ventas");
        assertThat(conocido.campos()).extracting("nombre", "bloqueRelacionado")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Monto", null),
                        org.assertj.core.groups.Tuple.tuple("Cliente", "Clientes"));
    }
}
