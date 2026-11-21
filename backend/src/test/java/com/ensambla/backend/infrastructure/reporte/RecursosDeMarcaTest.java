package com.ensambla.backend.infrastructure.reporte;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecursosDeMarcaTest {

    private RecursosDeMarca recursos;
    private BaseFont fuente;
    private Image imagen;

    @Test
    @DisplayName("Lee la tipografía de la marca desde el classpath")
    void leeLaTipografiaDeLaMarcaDesdeElClasspath() {
        dadosLosRecursosDeLaMarca();

        cuandoSePideLaFuente("plus-jakarta-sans-bold.ttf");

        assertThat(fuente.getPostscriptFontName()).contains("PlusJakartaSans");
    }

    @Test
    @DisplayName("Lee las imágenes de Sami desde el classpath")
    void leeLasImagenesDeSamiDesdeElClasspath() {
        dadosLosRecursosDeLaMarca();

        cuandoSePideLaImagen("sami-guino.png");

        assertThat(imagen.getWidth()).isPositive();
    }

    @Test
    @DisplayName("Si falta una fuente avisa cuál no se pudo leer")
    void siFaltaUnaFuenteAvisaCualNoSePudoLeer() {
        dadosLosRecursosSinArchivos();

        assertThatThrownBy(() -> cuandoSePideLaFuente("plus-jakarta-sans-bold.ttf"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo leer el recurso de marca plus-jakarta-sans-bold.ttf");
    }

    @Test
    @DisplayName("Si falta una imagen avisa cuál no se pudo leer")
    void siFaltaUnaImagenAvisaCualNoSePudoLeer() {
        dadosLosRecursosSinArchivos();

        assertThatThrownBy(() -> cuandoSePideLaImagen("sami-guino.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo leer el recurso de marca sami-guino.png");
    }

    private void dadosLosRecursosDeLaMarca() {
        recursos = new RecursosDeMarca();
    }

    private void dadosLosRecursosSinArchivos() {
        recursos = new RecursosDeMarca(archivo -> new ClassPathResource("reporte/sin-archivos/" + archivo));
    }

    private void cuandoSePideLaFuente(String archivo) {
        fuente = recursos.fuente(archivo);
    }

    private void cuandoSePideLaImagen(String archivo) {
        imagen = recursos.imagen(archivo);
    }
}
