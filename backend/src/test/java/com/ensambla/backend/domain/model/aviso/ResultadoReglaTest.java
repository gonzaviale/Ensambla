package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadoReglaTest {

    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("La entidad de un registro lleva el prefijo registro")
    void entidadDeRegistroShouldUsePrefix() {
        assertThat(ResultadoRegla.entidadDeRegistro(REGISTRO_ID)).isEqualTo("registro:" + REGISTRO_ID);
    }

    @Test
    @DisplayName("La huella empieza con el tipo y la entidad")
    void huellaParaShouldStartWithTipoAndEntidad() {
        ResultadoRegla resultado = givenResultado(Map.of("cantidad", BigDecimal.TWO));

        assertThat(resultado.huellaPara(TipoAviso.STOCK)).startsWith("STOCK:registro:1:");
    }

    @Test
    @DisplayName("La huella no depende del orden de los datos ni de ceros decimales")
    void huellaParaShouldIgnoreOrderAndTrailingZeros() {
        Map<String, Object> uno = new LinkedHashMap<>();
        uno.put("cantidad", new BigDecimal("2.0"));
        uno.put("minimo", 5);
        Map<String, Object> otro = new LinkedHashMap<>();
        otro.put("minimo", 5);
        otro.put("cantidad", new BigDecimal("2"));

        assertThat(givenResultado(uno).huellaPara(TipoAviso.STOCK))
                .isEqualTo(givenResultado(otro).huellaPara(TipoAviso.STOCK));
    }

    @Test
    @DisplayName("La huella cambia cuando cambian los datos de origen")
    void huellaParaShouldChangeWhenDatosChange() {
        ResultadoRegla antes = givenResultado(Map.of("cantidad", new BigDecimal("2")));
        ResultadoRegla despues = givenResultado(Map.of("cantidad", new BigDecimal("1")));

        assertThat(antes.huellaPara(TipoAviso.STOCK)).isNotEqualTo(despues.huellaPara(TipoAviso.STOCK));
    }

    private static ResultadoRegla givenResultado(Map<String, Object> datos) {
        return new ResultadoRegla(null, "registro:1", (short) 2, "titulo", "detalle", datos, List.of(), List.of());
    }
}
