package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.support.ProcesamientoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchivoCuadernoLocalAdapterTest {

    @TempDir
    private Path raiz;

    private ArchivoCuadernoLocalAdapter adapter;
    private String clave;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectory(raiz.resolve(ProcesamientoTestData.NEGOCIO_ID.toString()));
        clave = ProcesamientoTestData.NEGOCIO_ID + "/pagina.jpg";
        adapter = new ArchivoCuadernoLocalAdapter(raiz, 3);
    }

    @Test
    @DisplayName("Lee únicamente los bytes privados del negocio")
    void leerShouldReadOwnPrivateFile() throws Exception {
        Files.write(raiz.resolve(clave), new byte[]{1, 2, 3});

        byte[] bytes = adapter.leer(ProcesamientoTestData.NEGOCIO_ID, clave);

        assertThat(bytes).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Rechaza una ruta que sale de la carpeta del negocio")
    void leerShouldRejectTraversal() throws Exception {
        Files.write(raiz.resolve("otra.jpg"), new byte[]{1});

        assertThatThrownBy(() -> adapter.leer(ProcesamientoTestData.NEGOCIO_ID,
                ProcesamientoTestData.NEGOCIO_ID + "/../otra.jpg"))
                .isInstanceOf(LecturaCuadernoException.class).hasMessageContaining("no pertenece");
    }

    @Test
    @DisplayName("Rechaza fotos vacías")
    void leerShouldRejectEmptyFile() throws Exception {
        Files.write(raiz.resolve(clave), new byte[0]);

        assertThatThrownBy(() -> adapter.leer(ProcesamientoTestData.NEGOCIO_ID, clave))
                .hasMessageContaining("vacía");
    }

    @Test
    @DisplayName("Rechaza fotos que exceden el máximo")
    void leerShouldRejectOversizedFile() throws Exception {
        Files.write(raiz.resolve(clave), new byte[]{1, 2, 3, 4});

        assertThatThrownBy(() -> adapter.leer(ProcesamientoTestData.NEGOCIO_ID, clave))
                .hasMessageContaining("tamaño");
    }

    @Test
    @DisplayName("Una foto ausente produce un error permanente sin exponer su ruta")
    void leerShouldRejectMissingFile() {
        assertThatThrownBy(() -> adapter.leer(ProcesamientoTestData.NEGOCIO_ID, clave))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessage("No se pudo acceder a la foto privada del lote");
    }

    @Test
    @DisplayName("Guarda la foto dentro de la carpeta del negocio creando las subcarpetas necesarias")
    void guardarShouldWriteFileInsideNegocioFolder() throws Exception {
        adapter.guardar(ProcesamientoTestData.NEGOCIO_ID, ProcesamientoTestData.NEGOCIO_ID + "/fotos/a.jpg",
                new byte[]{7, 8});

        assertThat(Files.readAllBytes(raiz.resolve(ProcesamientoTestData.NEGOCIO_ID + "/fotos/a.jpg")))
                .containsExactly(7, 8);
    }

    @Test
    @DisplayName("No guarda una foto fuera de la carpeta del negocio")
    void guardarShouldRejectPathOutsideNegocioFolder() {
        assertThatThrownBy(() -> adapter.guardar(ProcesamientoTestData.NEGOCIO_ID,
                ProcesamientoTestData.NEGOCIO_ID + "/../otra.jpg", new byte[]{1}))
                .isInstanceOf(LecturaCuadernoException.class).hasMessageContaining("no pertenece");
    }

    @Test
    @DisplayName("Si no puede escribir en disco informa un error permanente sin exponer la ruta")
    void guardarShouldFailWhenFolderCannotBeCreated() throws Exception {
        Files.createFile(raiz.resolve(ProcesamientoTestData.NEGOCIO_ID + "/fotos"));

        assertThatThrownBy(() -> adapter.guardar(ProcesamientoTestData.NEGOCIO_ID,
                ProcesamientoTestData.NEGOCIO_ID + "/fotos/a.jpg", new byte[]{1}))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessage("No se pudo guardar la foto privada del lote");
    }

    @Test
    @DisplayName("Elimina la foto del negocio")
    void eliminarShouldDeleteOwnFile() throws Exception {
        Files.write(raiz.resolve(clave), new byte[]{1});

        adapter.eliminar(ProcesamientoTestData.NEGOCIO_ID, clave);

        assertThat(raiz.resolve(clave)).doesNotExist();
    }

    @Test
    @DisplayName("Eliminar una foto que ya no existe no falla")
    void eliminarShouldIgnoreMissingFile() {
        assertThatCode(() -> adapter.eliminar(ProcesamientoTestData.NEGOCIO_ID, clave)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("No elimina un archivo fuera de la carpeta del negocio")
    void eliminarShouldRejectPathOutsideNegocioFolder() throws Exception {
        Files.write(raiz.resolve("otra.jpg"), new byte[]{1});

        assertThatThrownBy(() -> adapter.eliminar(ProcesamientoTestData.NEGOCIO_ID,
                ProcesamientoTestData.NEGOCIO_ID + "/../otra.jpg"))
                .isInstanceOf(LecturaCuadernoException.class).hasMessageContaining("no pertenece");
        assertThat(raiz.resolve("otra.jpg")).exists();
    }

    @Test
    @DisplayName("Si no puede eliminar la foto informa un error permanente")
    void eliminarShouldFailWhenPathIsNotEmptyFolder() throws Exception {
        Files.createDirectories(raiz.resolve(ProcesamientoTestData.NEGOCIO_ID + "/carpeta"));
        Files.write(raiz.resolve(ProcesamientoTestData.NEGOCIO_ID + "/carpeta/x.jpg"), new byte[]{1});

        assertThatThrownBy(() -> adapter.eliminar(ProcesamientoTestData.NEGOCIO_ID,
                ProcesamientoTestData.NEGOCIO_ID + "/carpeta"))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessage("No se pudo eliminar la foto privada del lote");
    }
}
