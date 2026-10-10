package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.DireccionOrden;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaRegistrosTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final List<Campo> CAMPOS = List.of(Campo.builder().id(CAMPO_ID).build());

    @Test
    @DisplayName("Sin parámetros pide la primera página de 20 por fecha de carga descendente")
    void armarShouldUseDefaultsWhenNothingIsSent() {
        ConsultaRegistros consulta = armar(null, null, null, null, null);

        assertThat(consulta.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(consulta.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(consulta.texto()).isNull();
        assertThat(consulta.campoOrdenId()).isNull();
        assertThat(consulta.direccion()).isEqualTo(DireccionOrden.DESC);
        assertThat(consulta.pagina()).isZero();
        assertThat(consulta.tamanio()).isEqualTo(20);
    }

    @Test
    @DisplayName("Ignora el texto en blanco y quita los espacios del texto a buscar")
    void armarShouldNormalizeTexto() {
        assertThat(armar("   ", null, null, null, null).texto()).isNull();
        assertThat(armar("  harina ", null, null, null, null).texto()).isEqualTo("harina");
    }

    @Test
    @DisplayName("Ordena por fecha de carga cuando el orden es vacío o creadoEn")
    void armarShouldOrderByCargaWhenOrdenIsBlankOrCreadoEn() {
        assertThat(armar(null, "  ", null, null, null).campoOrdenId()).isNull();
        assertThat(armar(null, "creadoEn", null, null, null).campoOrdenId()).isNull();
    }

    @Test
    @DisplayName("Ordena por un campo del bloque identificado por su id")
    void armarShouldOrderByCampoOfTheBloque() {
        ConsultaRegistros consulta = armar(null, CAMPO_ID.toString(), "asc", null, null);

        assertThat(consulta.campoOrdenId()).isEqualTo(CAMPO_ID);
        assertThat(consulta.direccion()).isEqualTo(DireccionOrden.ASC);
    }

    @Test
    @DisplayName("Rechaza ordenar por un campo que no es del bloque")
    void armarShouldRejectUnknownCampo() {
        String ajeno = UUID.randomUUID().toString();

        assertThatThrownBy(() -> armar(null, ajeno, null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede ordenar por ese campo");
    }

    @Test
    @DisplayName("Rechaza una dirección distinta de asc y desc")
    void armarShouldRejectInvalidDireccion() {
        assertThatThrownBy(() -> armar(null, null, "arriba", null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Corrige una página negativa a la primera")
    void armarShouldClampNegativePagina() {
        assertThat(armar(null, null, null, -3, null).pagina()).isZero();
        assertThat(armar(null, null, null, 4, null).pagina()).isEqualTo(4);
    }

    @Test
    @DisplayName("Acota el tamaño de página entre 1 y 100")
    void armarShouldClampTamanio() {
        assertThat(armar(null, null, null, null, 0).tamanio()).isEqualTo(1);
        assertThat(armar(null, null, null, null, 500).tamanio()).isEqualTo(100);
        assertThat(armar(null, null, null, null, 50).tamanio()).isEqualTo(50);
    }

    // --- act ---
    private ConsultaRegistros armar(String texto, String orden, String direccion, Integer pagina, Integer tamanio) {
        return ConsultaRegistros.armar(NEGOCIO_ID, BLOQUE_ID, CAMPOS, texto, orden, direccion, pagina, tamanio);
    }
}
