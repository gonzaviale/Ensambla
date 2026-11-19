package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.plantilla;
import static com.ensambla.backend.support.ProyectoTestData.proyecto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ProyectoTest {

    private static final OffsetDateTime MANANA = AHORA.plusDays(1);

    private PlantillaProyecto plantilla;
    private Proyecto proyecto;
    private Bloque bloqueAgregado;
    private Throwable rechazo;

    @Test
    @DisplayName("Crea un proyecto en blanco con el nombre normalizado y sin estructura")
    void creaUnProyectoEnBlancoConElNombreNormalizadoYSinEstructura() {
        cuandoSeCreaUnProyectoEnBlancoLlamado("  Mi negocio  ");

        entoncesTieneElNombreNormalizadoYLasFechasDeCreacion();
        entoncesNoTienePlantillaNiDescripcionNiBloques();
    }

    @Test
    @DisplayName("Copia los bloques y campos con orden y valores propios sin alterar la plantilla")
    void copiaLosBloquesYCamposConOrdenYValoresPropiosSinAlterarLaPlantilla() {
        dadoUnaPlantillaDeInventario();

        cuandoSeCreaUnProyectoDesdeLaPlantilla();

        entoncesCopiaLosDatosDeLaPlantillaYElOrdenDeSusBloques();
        entoncesCopiaElPrimerBloqueConSusCampos();
        entoncesCopiaElPrimerCampoDelPrimerBloque();
        entoncesCopiaElValorInicialDelSegundoCampo();
        entoncesRenombrarLaCopiaNoAlteraLaPlantilla();
    }

    @Test
    @DisplayName("Renombra preservando la fecha de creación y actualizando la de modificación")
    void renombraPreservandoLaFechaDeCreacionYActualizandoLaDeModificacion() {
        dadoUnProyectoConProductosYUnBloqueVacio();

        cuandoSeRenombraElProyecto("  Nuevo  ");

        entoncesSeLlamaNuevoYConservaLaFechaDeCreacion();
    }

    @Test
    @DisplayName("Agrega el bloque al final, en el negocio y el proyecto, sin guardarlo todavía")
    void agregaElBloqueAlFinalEnElNegocioYElProyectoSinGuardarloTodavia() {
        dadoUnProyectoConSuPrimerBloqueEnLaPosicionCuatro();

        cuandoSeAgregaElBloque(" Clientes ");

        entoncesElBloqueNuevoEstaEnLaPosicionCincoSinGuardar();
        entoncesElProyectoNoCambiaSusBloques();
    }

    @Test
    @DisplayName("El primer bloque de un proyecto en blanco queda en la posición cero")
    void elPrimerBloqueDeUnProyectoEnBlancoQuedaEnLaPosicionCero() {
        dadoUnProyectoEnBlanco();

        cuandoSeAgregaElBloque("Productos");

        entoncesElBloqueNuevoEstaEnLaPosicionCero();
    }

    @Test
    @DisplayName("Rechaza un bloque con el nombre de otro aunque cambien mayúsculas y espacios")
    void rechazaUnBloqueConElNombreDeOtroAunqueCambienMayusculasYEspacios() {
        dadoUnProyectoConProductosYUnBloqueVacio();

        cuandoSeIntentaAgregarElBloque("  PRODUCTOS ");

        entoncesSeRechazaPorNombreRepetido("Ya hay un bloque llamado \"PRODUCTOS\" en este proyecto");
    }

    @Test
    @DisplayName("Un bloque puede quedarse con su propio nombre al renombrarse")
    void unBloquePuedeQuedarseConSuPropioNombreAlRenombrarse() {
        dadoUnProyectoConProductosYUnBloqueVacio();

        cuandoSeValidaElNombreProductosParaElBloque(primerBloqueId());

        entoncesNoSeRechazaElNombre();
    }

    @Test
    @DisplayName("Renombrar con el nombre de otro bloque del proyecto se rechaza")
    void renombrarConElNombreDeOtroBloqueDelProyectoSeRechaza() {
        dadoUnProyectoConProductosYUnBloqueVacio();

        cuandoSeValidaElNombreProductosParaElBloque(segundoBloqueId());

        entoncesSeRechazaPorNombreRepetido("Ya hay un bloque llamado \"Productos\" en este proyecto");
    }

    // --- dado ---
    private void dadoUnaPlantillaDeInventario() {
        plantilla = plantilla();
    }

    private void dadoUnProyectoConProductosYUnBloqueVacio() {
        proyecto = proyecto();
    }

    private void dadoUnProyectoConSuPrimerBloqueEnLaPosicionCuatro() {
        proyecto = proyecto();
        proyecto.getBloques().getFirst().setPosicion((short) 4);
    }

    private void dadoUnProyectoEnBlanco() {
        proyecto = Proyecto.enBlanco(NEGOCIO_ID, "Vacío", null, AHORA);
    }

    // --- cuando ---
    private void cuandoSeCreaUnProyectoEnBlancoLlamado(String nombre) {
        proyecto = Proyecto.enBlanco(NEGOCIO_ID, nombre, null, AHORA);
    }

    private void cuandoSeCreaUnProyectoDesdeLaPlantilla() {
        proyecto = Proyecto.desdePlantilla(NEGOCIO_ID, plantilla, AHORA);
    }

    private void cuandoSeRenombraElProyecto(String nombre) {
        proyecto.renombrar(nombre, MANANA);
    }

    private void cuandoSeAgregaElBloque(String nombre) {
        bloqueAgregado = proyecto.agregarBloque(nombre, MANANA);
    }

    private void cuandoSeIntentaAgregarElBloque(String nombre) {
        rechazo = catchThrowable(() -> proyecto.agregarBloque(nombre, AHORA));
    }

    private void cuandoSeValidaElNombreProductosParaElBloque(BloqueId bloqueId) {
        rechazo = catchThrowable(() -> proyecto.validarNombreDeBloqueLibre("Productos", bloqueId));
    }

    private BloqueId primerBloqueId() {
        return proyecto.getBloques().getFirst().getId();
    }

    private BloqueId segundoBloqueId() {
        return proyecto.getBloques().get(1).getId();
    }

    // --- entonces ---
    private void entoncesTieneElNombreNormalizadoYLasFechasDeCreacion() {
        assertThat(proyecto.getNombre()).isEqualTo("Mi negocio");
        assertThat(proyecto.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(proyecto.getCreadoEn()).isEqualTo(AHORA);
        assertThat(proyecto.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesNoTienePlantillaNiDescripcionNiBloques() {
        assertThat(proyecto.getDescripcion()).isNull();
        assertThat(proyecto.getPlantillaId()).isNull();
        assertThat(proyecto.getBloques()).isEmpty();
    }

    private void entoncesCopiaLosDatosDeLaPlantillaYElOrdenDeSusBloques() {
        assertThat(proyecto.getPlantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(proyecto.getDescripcion()).isEqualTo("Mis productos");
        assertThat(proyecto.getBloques()).extracting("nombre").containsExactly("Productos", "Vacío");
    }

    private void entoncesCopiaElPrimerBloqueConSusCampos() {
        Bloque bloque = proyecto.getBloques().getFirst();
        assertThat(bloque.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(bloque.getPosicion()).isZero();
        assertThat(bloque.getCreadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(bloque.getCampos()).extracting("nombre").containsExactly("Nombre", "Rubro");
    }

    private void entoncesCopiaElPrimerCampoDelPrimerBloque() {
        Campo campo = proyecto.getBloques().getFirst().getCampos().getFirst();
        assertThat(campo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(campo.getPosicion()).isZero();
        assertThat(campo.getObligatorio()).isTrue();
        assertThat(campo.getOpciones()).isEmpty();
        assertThat(campo.getCreadoEn()).isEqualTo(AHORA);
        assertThat(campo.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesCopiaElValorInicialDelSegundoCampo() {
        assertThat(proyecto.getBloques().getFirst().getCampos().get(1).getValorInicial()).isEqualTo("Insumos");
    }

    private void entoncesRenombrarLaCopiaNoAlteraLaPlantilla() {
        proyecto.getBloques().getFirst().setNombre("Otra cosa");
        assertThat(plantilla.estructura().bloques().getFirst().nombre()).isEqualTo("Productos");
    }

    private void entoncesSeLlamaNuevoYConservaLaFechaDeCreacion() {
        assertThat(proyecto.getNombre()).isEqualTo("Nuevo");
        assertThat(proyecto.getCreadoEn()).isEqualTo(AHORA);
        assertThat(proyecto.getActualizadoEn()).isEqualTo(MANANA);
    }

    private void entoncesElBloqueNuevoEstaEnLaPosicionCincoSinGuardar() {
        assertThat(bloqueAgregado.getId()).isNull();
        assertThat(bloqueAgregado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(bloqueAgregado.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(bloqueAgregado.getNombre()).isEqualTo("Clientes");
        assertThat(bloqueAgregado.getPosicion()).isEqualTo((short) 5);
        assertThat(bloqueAgregado.getCreadoEn()).isEqualTo(MANANA);
    }

    private void entoncesElProyectoNoCambiaSusBloques() {
        assertThat(proyecto.getBloques()).hasSize(2);
    }

    private void entoncesElBloqueNuevoEstaEnLaPosicionCero() {
        assertThat(bloqueAgregado.getPosicion()).isZero();
    }

    private void entoncesSeRechazaPorNombreRepetido(String mensaje) {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class).hasMessage(mensaje);
    }

    private void entoncesNoSeRechazaElNombre() {
        assertThat(rechazo).isNull();
    }
}
