package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ConfirmacionLecturaTest {

    private static final ProyectoId PROYECTO_ID = ProyectoId.de("00000000-0000-4000-8000-000000000021");
    private static final BloqueId BLOQUE_ID = BloqueId.de("00000000-0000-4000-8000-000000000022");

    private ConfirmacionLectura confirmacion;
    private Throwable rechazo;

    @Test
    @DisplayName("Acepta un proyecto y una tabla nuevos con columnas y filas")
    void aceptaUnProyectoYUnaTablaNuevosConColumnasYFilas() {
        dadoUnaConfirmacion(null, "Ventas", null, "Ventas", List.of(unaColumna("monto")));

        cuandoSeIntentaValidar();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Acepta una tabla nueva dentro de un proyecto existente sin nombre de proyecto")
    void aceptaUnaTablaNuevaDentroDeUnProyectoExistenteSinNombreDeProyecto() {
        dadoUnaConfirmacion(PROYECTO_ID, null, null, "Ventas", List.of(unaColumna("monto")));

        cuandoSeIntentaValidar();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Acepta sumar filas a un bloque existente sin nombres")
    void aceptaSumarFilasAUnBloqueExistenteSinNombres() {
        dadoUnaConfirmacion(null, null, BLOQUE_ID, null, List.of(unaColumna("monto")));

        cuandoSeIntentaValidar();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Rechaza una tabla nueva sin nombre")
    void rechazaUnaTablaNuevaSinNombre() {
        dadoUnaConfirmacion(PROYECTO_ID, null, null, "  ", List.of(unaColumna("monto")));

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Escribí un nombre para la tabla");
    }

    @Test
    @DisplayName("Rechaza una tabla nueva sin nombre cuando el nombre es nulo")
    void rechazaUnaTablaNuevaSinNombreCuandoElNombreEsNulo() {
        dadoUnaConfirmacion(PROYECTO_ID, null, null, null, List.of(unaColumna("monto")));

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Escribí un nombre para la tabla");
    }

    @Test
    @DisplayName("Rechaza un proyecto nuevo sin nombre")
    void rechazaUnProyectoNuevoSinNombre() {
        dadoUnaConfirmacion(null, "", null, "Ventas", List.of(unaColumna("monto")));

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Escribí un nombre para el proyecto");
    }

    @Test
    @DisplayName("Rechaza una confirmación sin columnas")
    void rechazaUnaConfirmacionSinColumnas() {
        dadoUnaConfirmacion(null, "Ventas", null, "Ventas", List.of());

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Dejá al menos una columna");
    }

    @Test
    @DisplayName("Rechaza columnas con la misma clave")
    void rechazaColumnasConLaMismaClave() {
        dadoUnaConfirmacion(null, "Ventas", null, "Ventas", List.of(unaColumna("monto"), unaColumna("monto")));

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Las columnas necesitan una clave distinta cada una");
    }

    @Test
    @DisplayName("Rechaza una columna sin clave")
    void rechazaUnaColumnaSinClave() {
        dadoUnaConfirmacion(null, "Ventas", null, "Ventas", List.of(unaColumna(" ")));

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Las columnas necesitan una clave distinta cada una");
    }

    @Test
    @DisplayName("Rechaza más filas que el máximo por confirmación")
    void rechazaMasFilasQueElMaximoPorConfirmacion() {
        dadoUnaConfirmacionConMasFilasQueElMaximo();

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Podés guardar hasta 500 filas por vez");
    }

    @Test
    @DisplayName("Rechaza una columna de conexión nueva: las conexiones se arman en la tabla, no en la lectura")
    void rechazaUnaColumnaDeConexionNuevaLasConexionesSeArmanEnLaTablaNoEnLaLectura() {
        dadoUnaConfirmacion(null, null, BLOQUE_ID, null,
                List.of(new CampoConfirmado("cliente", null, "Cliente", "RELACION", List.of())));

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Una conexión con otra tabla no se puede crear acá: elegí una columna de tu tabla");
    }

    @Test
    @DisplayName("Acepta una columna de conexión que ya existe en la tabla")
    void aceptaUnaColumnaDeConexionQueYaExisteEnLaTabla() {
        dadoUnaConfirmacion(null, null, BLOQUE_ID, null,
                List.of(new CampoConfirmado("cliente", CampoId.nuevo(), "Cliente", "RELACION", List.of())));

        cuandoSeIntentaValidar();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Rechaza dos columnas que apuntan al mismo campo del bloque: una pisaría los valores de la otra")
    void rechazaDosColumnasQueApuntanAlMismoCampoDelBloque() {
        dadoUnaConfirmacionConDosColumnasDelMismoCampo();

        cuandoSeIntentaValidar();

        entoncesSeRechazaCon("Dos columnas no pueden guardarse en el mismo campo de la tabla");
    }

    @Test
    @DisplayName("Acepta columnas de campos distintos junto con columnas nuevas")
    void aceptaColumnasDeCamposDistintosJuntoConColumnasNuevas() {
        dadoUnaConfirmacionConColumnasExistentesYNuevas();

        cuandoSeIntentaValidar();

        entoncesSeAcepta();
    }

    // --- dado ---
    private void dadoUnaConfirmacion(ProyectoId proyectoId, String nombreProyecto, BloqueId bloqueId,
                                     String nombreBloque, List<CampoConfirmado> campos) {
        confirmacion = new ConfirmacionLectura(
                proyectoId, nombreProyecto, bloqueId, nombreBloque, campos, List.of(new FilaConfirmada(1, Map.of())));
    }

    private void dadoUnaConfirmacionConMasFilasQueElMaximo() {
        List<FilaConfirmada> filas = Collections.nCopies(
                ConfirmacionLectura.MAX_FILAS + 1, new FilaConfirmada(1, Map.of()));
        confirmacion = new ConfirmacionLectura(
                null, "Ventas", null, "Ventas", List.of(unaColumna("monto")), filas);
    }

    private void dadoUnaConfirmacionConDosColumnasDelMismoCampo() {
        CampoId campoId = CampoId.nuevo();
        dadoUnaConfirmacion(null, null, BLOQUE_ID, null, List.of(
                new CampoConfirmado("monto", campoId, "Monto", "NUMERO", List.of()),
                new CampoConfirmado("importe", campoId, "Monto", "NUMERO", List.of())));
    }

    private void dadoUnaConfirmacionConColumnasExistentesYNuevas() {
        dadoUnaConfirmacion(null, null, BLOQUE_ID, null, List.of(
                new CampoConfirmado("monto", CampoId.nuevo(), "Monto", "NUMERO", List.of()),
                new CampoConfirmado("fecha", CampoId.nuevo(), "Fecha", "FECHA", List.of()),
                new CampoConfirmado("telefono", null, "Teléfono", "TEXTO", List.of()),
                new CampoConfirmado("direccion", null, "Dirección", "TEXTO", List.of())));
    }

    private CampoConfirmado unaColumna(String clave) {
        return new CampoConfirmado(clave, null, "Monto", "NUMERO", List.of());
    }

    // --- cuando ---
    private void cuandoSeIntentaValidar() {
        rechazo = catchThrowable(confirmacion::validar);
    }

    // --- entonces ---
    private void entoncesSeAcepta() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaCon(String mensaje) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }
}
