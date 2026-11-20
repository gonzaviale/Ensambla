package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ArchivoCuadernoLocalAdapterTest {

    private static final NegocioId NEGOCIO_ID = ProcesamientoTestData.NEGOCIO_ID;
    private static final String FUERA_DE_LA_CARPETA = NEGOCIO_ID + "/../otra.jpg";
    private static final String CARPETA_DE_FOTOS = NEGOCIO_ID + "/fotos";

    @TempDir
    private Path raiz;

    private ArchivoCuadernoLocalAdapter adapter;
    private String clave;
    private byte[] bytes;
    private Throwable rechazo;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectory(raiz.resolve(NEGOCIO_ID.toString()));
        clave = NEGOCIO_ID + "/pagina.jpg";
        adapter = new ArchivoCuadernoLocalAdapter(raiz, 3);
    }

    @Test
    @DisplayName("Lee únicamente los bytes privados del negocio")
    void leeUnicamenteLosBytesPrivadosDelNegocio() throws Exception {
        dadoUnaFotoDelNegocioDeBytes(1, 2, 3);

        cuandoSeLeeLaFoto();

        entoncesSeLeenLosBytes(1, 2, 3);
    }

    @Test
    @DisplayName("Rechaza una ruta que sale de la carpeta del negocio")
    void rechazaUnaRutaQueSaleDeLaCarpetaDelNegocio() throws Exception {
        dadoUnArchivoAjenoFueraDeLaCarpetaDelNegocio();

        cuandoSeIntentaLeerFueraDeLaCarpeta();

        entoncesSeRechazaPorqueNoPerteneceAlNegocio();
    }

    @Test
    @DisplayName("Rechaza fotos vacías")
    void rechazaFotosVacias() throws Exception {
        dadoUnaFotoDelNegocioDeBytes();

        cuandoSeIntentaLeerLaFoto();

        entoncesSeRechazaConElMensajeQueContiene("vacía");
    }

    @Test
    @DisplayName("Rechaza fotos que exceden el máximo")
    void rechazaFotosQueExcedenElMaximo() throws Exception {
        dadoUnaFotoDelNegocioDeBytes(1, 2, 3, 4);

        cuandoSeIntentaLeerLaFoto();

        entoncesSeRechazaConElMensajeQueContiene("tamaño");
    }

    @Test
    @DisplayName("Una foto ausente produce un error permanente sin exponer su ruta")
    void unaFotoAusenteProduceUnErrorPermanenteSinExponerSuRuta() {
        cuandoSeIntentaLeerLaFoto();

        entoncesSeRechazaPermanentementeCon("No se pudo acceder a la foto privada del lote");
    }

    @Test
    @DisplayName("Guarda la foto dentro de la carpeta del negocio creando las subcarpetas necesarias")
    void guardaLaFotoDentroDeLaCarpetaDelNegocioCreandoLasSubcarpetasNecesarias() throws Exception {
        cuandoSeGuardaUnaFotoEnUnaSubcarpeta();

        entoncesLaFotoQuedaGuardadaEnLaSubcarpeta();
    }

    @Test
    @DisplayName("No guarda una foto fuera de la carpeta del negocio")
    void noGuardaUnaFotoFueraDeLaCarpetaDelNegocio() {
        cuandoSeIntentaGuardarFueraDeLaCarpeta();

        entoncesSeRechazaPorqueNoPerteneceAlNegocio();
    }

    @Test
    @DisplayName("Si no puede escribir en disco informa un error permanente sin exponer la ruta")
    void siNoPuedeEscribirEnDiscoInformaUnErrorPermanenteSinExponerLaRuta() throws Exception {
        dadoUnArchivoQueOcupaElLugarDeLaCarpetaDeFotos();

        cuandoSeIntentaGuardarUnaFotoEnLaCarpetaDeFotos();

        entoncesSeRechazaPermanentementeCon("No se pudo guardar la foto privada del lote");
    }

    @Test
    @DisplayName("Elimina la foto del negocio")
    void eliminaLaFotoDelNegocio() throws Exception {
        dadoUnaFotoDelNegocioDeBytes(1);

        cuandoSeEliminaLaFoto();

        entoncesLaFotoYaNoExiste();
    }

    @Test
    @DisplayName("Eliminar una foto que ya no existe no falla")
    void eliminarUnaFotoQueYaNoExisteNoFalla() {
        cuandoSeIntentaEliminarLaFoto();

        entoncesNoHuboFallo();
    }

    @Test
    @DisplayName("No elimina un archivo fuera de la carpeta del negocio")
    void noEliminaUnArchivoFueraDeLaCarpetaDelNegocio() throws Exception {
        dadoUnArchivoAjenoFueraDeLaCarpetaDelNegocio();

        cuandoSeIntentaEliminarFueraDeLaCarpeta();

        entoncesSeRechazaPorqueNoPerteneceAlNegocio();
        entoncesElArchivoAjenoSigueExistiendo();
    }

    @Test
    @DisplayName("Si no puede eliminar la foto informa un error permanente")
    void siNoPuedeEliminarLaFotoInformaUnErrorPermanente() throws Exception {
        dadoUnaCarpetaNoVaciaDondeSeEsperabaUnaFoto();

        cuandoSeIntentaEliminarLaCarpeta();

        entoncesSeRechazaPermanentementeCon("No se pudo eliminar la foto privada del lote");
    }

    // --- dado ---

    private void dadoUnaFotoDelNegocioDeBytes(int... contenido) throws Exception {
        byte[] datos = new byte[contenido.length];
        for (int i = 0; i < contenido.length; i++) {
            datos[i] = (byte) contenido[i];
        }
        Files.write(raiz.resolve(clave), datos);
    }

    private void dadoUnArchivoAjenoFueraDeLaCarpetaDelNegocio() throws Exception {
        Files.write(raiz.resolve("otra.jpg"), new byte[]{1});
    }

    private void dadoUnArchivoQueOcupaElLugarDeLaCarpetaDeFotos() throws Exception {
        Files.createFile(raiz.resolve(CARPETA_DE_FOTOS));
    }

    private void dadoUnaCarpetaNoVaciaDondeSeEsperabaUnaFoto() throws Exception {
        Files.createDirectories(raiz.resolve(NEGOCIO_ID + "/carpeta"));
        Files.write(raiz.resolve(NEGOCIO_ID + "/carpeta/x.jpg"), new byte[]{1});
    }

    // --- cuando ---

    private void cuandoSeLeeLaFoto() {
        bytes = adapter.leer(NEGOCIO_ID, clave);
    }

    private void cuandoSeIntentaLeerLaFoto() {
        rechazo = catchThrowable(this::cuandoSeLeeLaFoto);
    }

    private void cuandoSeIntentaLeerFueraDeLaCarpeta() {
        rechazo = catchThrowable(() -> adapter.leer(NEGOCIO_ID, FUERA_DE_LA_CARPETA));
    }

    private void cuandoSeGuardaUnaFotoEnUnaSubcarpeta() {
        adapter.guardar(NEGOCIO_ID, CARPETA_DE_FOTOS + "/a.jpg", new byte[]{7, 8});
    }

    private void cuandoSeIntentaGuardarFueraDeLaCarpeta() {
        rechazo = catchThrowable(() -> adapter.guardar(NEGOCIO_ID, FUERA_DE_LA_CARPETA, new byte[]{1}));
    }

    private void cuandoSeIntentaGuardarUnaFotoEnLaCarpetaDeFotos() {
        rechazo = catchThrowable(() -> adapter.guardar(NEGOCIO_ID, CARPETA_DE_FOTOS + "/a.jpg", new byte[]{1}));
    }

    private void cuandoSeEliminaLaFoto() {
        adapter.eliminar(NEGOCIO_ID, clave);
    }

    private void cuandoSeIntentaEliminarLaFoto() {
        rechazo = catchThrowable(this::cuandoSeEliminaLaFoto);
    }

    private void cuandoSeIntentaEliminarFueraDeLaCarpeta() {
        rechazo = catchThrowable(() -> adapter.eliminar(NEGOCIO_ID, FUERA_DE_LA_CARPETA));
    }

    private void cuandoSeIntentaEliminarLaCarpeta() {
        rechazo = catchThrowable(() -> adapter.eliminar(NEGOCIO_ID, NEGOCIO_ID + "/carpeta"));
    }

    // --- entonces ---

    private void entoncesSeLeenLosBytes(int... esperados) {
        assertThat(bytes).containsExactly(esperados);
    }

    private void entoncesSeRechazaPorqueNoPerteneceAlNegocio() {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class).hasMessageContaining("no pertenece");
    }

    private void entoncesSeRechazaConElMensajeQueContiene(String texto) {
        assertThat(rechazo).hasMessageContaining(texto);
    }

    private void entoncesSeRechazaPermanentementeCon(String mensaje) {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class).hasMessage(mensaje);
    }

    private void entoncesLaFotoQuedaGuardadaEnLaSubcarpeta() throws Exception {
        assertThat(Files.readAllBytes(raiz.resolve(CARPETA_DE_FOTOS + "/a.jpg"))).containsExactly(7, 8);
    }

    private void entoncesLaFotoYaNoExiste() {
        assertThat(raiz.resolve(clave)).doesNotExist();
    }

    private void entoncesNoHuboFallo() {
        assertThat(rechazo).isNull();
    }

    private void entoncesElArchivoAjenoSigueExistiendo() {
        assertThat(raiz.resolve("otra.jpg")).exists();
    }
}
