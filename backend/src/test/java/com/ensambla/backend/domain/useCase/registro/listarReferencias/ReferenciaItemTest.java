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

    private List<ReferenciaItem> referencias;

    @Test
    @DisplayName("Arma el id y la etiqueta de cada registro conservando el orden")
    void armaElIdYLaEtiquetaDeCadaRegistroConservandoElOrden() {
        List<Registro> registros = dadoLosRegistrosAnaYBeto();

        cuandoSeArmanLasReferencias(registros);

        entoncesLasReferenciasSonAnaYBetoEnOrden();
    }

    @Test
    @DisplayName("Sin registros devuelve una lista vacía")
    void sinRegistrosDevuelveUnaListaVacia() {
        cuandoSeArmanLasReferencias(List.of());

        entoncesNoHayReferencias();
    }

    // --- dado ---
    private List<Registro> dadoLosRegistrosAnaYBeto() {
        return List.of(unRegistro(ANA_ID.valor(), "Ana"), unRegistro(BETO_ID.valor(), "Beto"));
    }

    private Registro unRegistro(UUID id, String nombre) {
        return Registro.builder().id(RegistroId.de(id)).valores(Map.of(NOMBRE_ID.toString(), nombre)).build();
    }

    // --- cuando ---
    private void cuandoSeArmanLasReferencias(List<Registro> registros) {
        referencias = ReferenciaItem.de(registros, CAMPOS);
    }

    // --- entonces ---
    private void entoncesLasReferenciasSonAnaYBetoEnOrden() {
        assertThat(referencias)
                .containsExactly(new ReferenciaItem(ANA_ID, "Ana"), new ReferenciaItem(BETO_ID, "Beto"));
    }

    private void entoncesNoHayReferencias() {
        assertThat(referencias).isEmpty();
    }
}
