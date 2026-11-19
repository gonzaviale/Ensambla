package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ColumnasDePrueba.unaColumna;
import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.plantilla;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ProyectoColumnasTest {

    private Proyecto proyecto;
    private Throwable rechazo;

    @Test
    @DisplayName("Suma las columnas al final del primer bloque, con su tipo, opciones y posición")
    void sumaLasColumnasAlFinalDelPrimerBloqueConSuTipoOpcionesYPosicion() {
        dadoUnProyectoCopiadoDeLaPlantilla();

        cuandoSeAgreganSaldoPendienteYFormaDePago();

        entoncesElPrimerBloqueTerminaConLasDosColumnasNuevas();
        entoncesElSegundoBloqueSigueSinCampos();
    }

    @Test
    @DisplayName("Sin columnas no cambia nada, ni siquiera si el proyecto no tiene bloques")
    void sinColumnasNoCambiaNadaNiSiquieraSiElProyectoNoTieneBloques() {
        dadoUnProyectoEnBlanco();

        cuandoSeAgreganCeroColumnas();

        entoncesElProyectoSigueSinBloques();
    }

    @Test
    @DisplayName("Sin columnas un proyecto con bloques conserva sus campos")
    void sinColumnasUnProyectoConBloquesConservaSusCampos() {
        dadoUnProyectoCopiadoDeLaPlantilla();

        cuandoSeAgreganCeroColumnas();

        entoncesElPrimerBloqueSigueConSusDosCampos();
    }

    @Test
    @DisplayName("Un proyecto sin bloques no tiene dónde agregar columnas")
    void unProyectoSinBloquesNoTieneDondeAgregarColumnas() {
        dadoUnProyectoEnBlanco();

        cuandoSeIntentaAgregarLaColumna(unaColumna("Saldo", "NUMERO"));

        entoncesSeRechazaPorFaltaDeBloqueConMensaje("La plantilla no tiene una tabla donde agregar columnas");
    }

    @Test
    @DisplayName("Rechaza una columna con el nombre de una que la plantilla ya trae, sin distinguir mayúsculas")
    void rechazaUnaColumnaConElNombreDeUnaQueLaPlantillaYaTraeSinDistinguirMayusculas() {
        dadoUnProyectoCopiadoDeLaPlantilla();

        cuandoSeIntentaAgregarLaColumna(unaColumna(" nombre ", "TEXTO"));

        entoncesSeRechazaPorNombreRepetidoConMensaje("Ya hay una columna llamada \"nombre\"");
    }

    @Test
    @DisplayName("Rechaza dos columnas nuevas con el mismo nombre")
    void rechazaDosColumnasNuevasConElMismoNombre() {
        dadoUnProyectoCopiadoDeLaPlantilla();

        cuandoSeIntentaAgregarLasColumnas(unaColumna("Saldo", "NUMERO"), unaColumna("SALDO", "TEXTO"));

        entoncesSeRechazaPorNombreRepetido();
    }

    @Test
    @DisplayName("No admite conexiones y dice cuál de las columnas es")
    void noAdmiteConexionesYDiceCualDeLasColumnasEs() {
        dadoUnProyectoCopiadoDeLaPlantilla();

        cuandoSeIntentaAgregarLasColumnas(unaColumna("Total", "NUMERO"), unaColumna("Cliente", "RELACION"));

        entoncesElErrorSenalaSoloLaClave("columnas[1].tipo");
    }

    @Test
    @DisplayName("No admite fórmulas y dice cuál de las columnas es")
    void noAdmiteFormulasYDiceCualDeLasColumnasEs() {
        dadoUnProyectoCopiadoDeLaPlantilla();

        cuandoSeIntentaAgregarLaColumna(unaColumna("Cuenta", "FORMULA"));

        entoncesElErrorSenalaSoloLaClave("columnas[0].tipo");
    }

    @Test
    @DisplayName("Los errores de una columna llevan su posición para ubicarla")
    void losErroresDeUnaColumnaLlevanSuPosicionParaUbicarla() {
        dadoUnProyectoCopiadoDeLaPlantilla();

        cuandoSeIntentaAgregarUnaColumnaConOpcionesRepetidasSeguidaDeUnaSinNombre();

        entoncesElErrorSenalaSoloLaClave("columnas[0].opciones");
        entoncesElErrorNoSenalaLaClave("columnas[1].nombre");
    }

    @Test
    @DisplayName("Una columna sin nombre se señala en la posición donde viene")
    void unaColumnaSinNombreSeSenalaEnLaPosicionDondeViene() {
        dadoUnProyectoCopiadoDeLaPlantilla();

        cuandoSeIntentaAgregarLasColumnas(unaColumna("Saldo", "NUMERO"), unaColumna(" ", "TEXTO"));

        entoncesElErrorSenalaSoloLaClave("columnas[1].nombre");
    }

    // --- dado ---
    private void dadoUnProyectoCopiadoDeLaPlantilla() {
        proyecto = Proyecto.desdePlantilla(NEGOCIO_ID, plantilla(), AHORA);
    }

    private void dadoUnProyectoEnBlanco() {
        proyecto = Proyecto.enBlanco(NEGOCIO_ID, "Mi negocio", null, AHORA);
    }

    // --- cuando ---
    private void cuandoSeAgreganSaldoPendienteYFormaDePago() {
        proyecto.agregarColumnas(List.of(
                unaColumna("  Saldo pendiente ", "NUMERO"),
                unaColumna("Forma de pago", "OPCION", List.of("Efectivo", "Tarjeta"))), AHORA);
    }

    private void cuandoSeAgreganCeroColumnas() {
        proyecto.agregarColumnas(List.of(), AHORA);
    }

    private void cuandoSeIntentaAgregarLaColumna(ConfiguracionCampo columna) {
        rechazo = catchThrowable(() -> proyecto.agregarColumnas(List.of(columna), AHORA));
    }

    private void cuandoSeIntentaAgregarLasColumnas(ConfiguracionCampo primera, ConfiguracionCampo segunda) {
        rechazo = catchThrowable(() -> proyecto.agregarColumnas(List.of(primera, segunda), AHORA));
    }

    private void cuandoSeIntentaAgregarUnaColumnaConOpcionesRepetidasSeguidaDeUnaSinNombre() {
        cuandoSeIntentaAgregarLasColumnas(
                unaColumna("Forma de pago", "OPCION", List.of("Efectivo", "efectivo")),
                unaColumna(" ", "TEXTO"));
    }

    // --- entonces ---
    private void entoncesElPrimerBloqueTerminaConLasDosColumnasNuevas() {
        List<Campo> campos = proyecto.getBloques().getFirst().getCampos();
        assertThat(campos).extracting(Campo::getNombre)
                .containsExactly("Nombre", "Rubro", "Saldo pendiente", "Forma de pago");
        assertThat(campos.get(2).getTipo()).isEqualTo(TipoCampo.NUMERO);
        assertThat(campos.get(2).getPosicion()).isEqualTo((short) 2);
        assertThat(campos.get(2).getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(campos.get(3).getOpciones()).containsExactly("Efectivo", "Tarjeta");
    }

    private void entoncesElSegundoBloqueSigueSinCampos() {
        assertThat(proyecto.getBloques().get(1).getCampos()).isEmpty();
    }

    private void entoncesElProyectoSigueSinBloques() {
        assertThat(proyecto.getBloques()).isEmpty();
    }

    private void entoncesElPrimerBloqueSigueConSusDosCampos() {
        assertThat(proyecto.getBloques().getFirst().getCampos()).hasSize(2);
    }

    private void entoncesSeRechazaPorFaltaDeBloqueConMensaje(String mensaje) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }

    private void entoncesSeRechazaPorNombreRepetidoConMensaje(String mensaje) {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class).hasMessage(mensaje);
    }

    private void entoncesSeRechazaPorNombreRepetido() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesElErrorSenalaSoloLaClave(String clave) {
        assertThat(rechazo).isInstanceOfSatisfying(CampoInvalidoException.class,
                error -> assertThat(error.getErrores()).containsOnlyKeys(clave));
    }

    private void entoncesElErrorNoSenalaLaClave(String clave) {
        assertThat(rechazo).isInstanceOfSatisfying(CampoInvalidoException.class,
                error -> assertThat(error.getErrores()).doesNotContainKey(clave));
    }
}
