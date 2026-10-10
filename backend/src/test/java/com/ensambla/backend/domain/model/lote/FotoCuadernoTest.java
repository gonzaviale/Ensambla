package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FotoCuadernoTest {

    private static final short POSICION = 2;

    @Test
    @DisplayName("Arma la foto con el tipo detectado, el tamaño y una clave dentro de la carpeta del negocio")
    void nuevaShouldBuildFotoInsideNegocioFolder() {
        FotoCuaderno foto = FotoCuaderno.nueva(NEGOCIO_ID, JPEG, POSICION, AHORA);

        assertThat(foto.getClaveAlmacenamiento()).startsWith("negocios/" + NEGOCIO_ID + "/fotos/").endsWith(".jpg");
        assertThat(foto.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(foto.getTipoContenido()).isEqualTo("image/jpeg");
        assertThat(foto.getTamanioBytes()).isEqualTo(JPEG.length);
        assertThat(foto.getRotacion()).isZero();
        assertThat(foto.getPosicion()).isEqualTo(POSICION);
        assertThat(foto.getCreadoEn()).isEqualTo(AHORA);
        assertThat(foto.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(foto.getContenido()).isEqualTo(JPEG);
    }

    @Test
    @DisplayName("Dos fotos iguales reciben claves distintas")
    void nuevaShouldGenerateUniqueKeys() {
        FotoCuaderno primera = FotoCuaderno.nueva(NEGOCIO_ID, JPEG, POSICION, AHORA);

        FotoCuaderno segunda = FotoCuaderno.nueva(NEGOCIO_ID, JPEG, POSICION, AHORA);

        assertThat(segunda.getClaveAlmacenamiento()).isNotEqualTo(primera.getClaveAlmacenamiento());
    }

    @Test
    @DisplayName("Rechaza un archivo vacío indicando qué foto es")
    void nuevaShouldRejectEmptyFile() {
        byte[] vacio = new byte[0];

        assertThatThrownBy(() -> FotoCuaderno.nueva(NEGOCIO_ID, vacio, POSICION, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La foto 3 está vacía");
    }

    @Test
    @DisplayName("Rechaza un archivo de más de 10 MB")
    void nuevaShouldRejectTooBigFile() {
        byte[] grande = new byte[(int) FotoCuaderno.MAX_BYTES + 1];

        assertThatThrownBy(() -> FotoCuaderno.nueva(NEGOCIO_ID, grande, POSICION, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La foto 3 supera el máximo de 10 MB");
    }

    @Test
    @DisplayName("Rechaza un archivo que no es una imagen ni un PDF")
    void nuevaShouldRejectUnsupportedType() {
        byte[] texto = "no soy una foto".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> FotoCuaderno.nueva(NEGOCIO_ID, texto, POSICION, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La foto 3 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF");
    }
}
