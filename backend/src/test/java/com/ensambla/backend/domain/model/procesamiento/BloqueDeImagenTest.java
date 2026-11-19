package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.procesamiento.BloqueDeImagen.CampoDeImagen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

class BloqueDeImagenTest {

    private BloqueDeImagen bloque;
    private Throwable rechazo;
    private CampoDeImagen sinOpciones;
    private CampoDeImagen otro;
    private BloqueConocido conocido;
    private BloqueLectura lectura;
    private Map<CampoId, String> claves;

    @Test
    @DisplayName("No se arma un bloque sin datos para leer")
    void noSeArmaUnBloqueSinDatosParaLeer() {
        cuandoSeIntentaArmarUnBloqueSinCampos();

        entoncesSeRechazaPorFaltaDeDatosParaLeer();
    }

    @Test
    @DisplayName("Un campo sin opciones informadas no tiene opciones y cada campo recibe su propio id")
    void unCampoSinOpcionesInformadasNoTieneOpcionesYCadaCampoRecibeSuPropioId() {
        cuandoSeArmanUnCampoSinOpcionesYOtroConOpciones();

        entoncesElPrimeroNoTieneOpcionesYLosDosTienenIdsDistintos();
    }

    @Test
    @DisplayName("Para la IA el bloque va con el mismo id y sus campos por nombre, tipo y opciones")
    void paraLaIaElBloqueVaConElMismoIdYSusCamposPorNombreTipoYOpciones() {
        dadoUnBloqueDeVentas();

        cuandoSeDescribeParaElLector();

        entoncesElLectorRecibeElBloqueConSusCamposSinLasClaves();
    }

    @Test
    @DisplayName("Para la propuesta el bloque lleva el id de cada campo")
    void paraLaPropuestaElBloqueLlevaElIdDeCadaCampo() {
        dadoUnBloqueDeVentas();

        cuandoSeDescribeParaLaPropuesta();

        entoncesLaPropuestaLlevaElIdDeCadaCampo();
    }

    @Test
    @DisplayName("La clave de quien llama se encuentra por el id del campo")
    void laClaveDeQuienLlamaSeEncuentraPorElIdDelCampo() {
        dadoUnBloqueDeVentas();

        cuandoSePidenLasClavesPorCampo();

        entoncesCadaIdDeCampoApuntaASuClave();
    }

    // --- dado ---
    private void dadoUnBloqueDeVentas() {
        bloque = BloqueDeImagen.de("Ventas", List.of(
                CampoDeImagen.nuevo("importe", "Monto", TipoCampo.NUMERO, List.of()),
                CampoDeImagen.nuevo("estado", "Estado", TipoCampo.OPCION, List.of("Pagó", "Debe"))));
    }

    // --- cuando ---
    private void cuandoSeIntentaArmarUnBloqueSinCampos() {
        rechazo = catchThrowable(() -> BloqueDeImagen.de("Ventas", List.of()));
    }

    private void cuandoSeArmanUnCampoSinOpcionesYOtroConOpciones() {
        sinOpciones = CampoDeImagen.nuevo("a", "A", TipoCampo.TEXTO, null);
        otro = CampoDeImagen.nuevo("a", "A", TipoCampo.TEXTO, List.of("x"));
    }

    private void cuandoSeDescribeParaElLector() {
        conocido = bloque.paraElLector();
    }

    private void cuandoSeDescribeParaLaPropuesta() {
        lectura = bloque.paraLaPropuesta();
    }

    private void cuandoSePidenLasClavesPorCampo() {
        claves = bloque.clavesPorCampo();
    }

    // --- entonces ---
    private void entoncesSeRechazaPorFaltaDeDatosParaLeer() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage("El bloque no tiene datos para leer");
    }

    private void entoncesElPrimeroNoTieneOpcionesYLosDosTienenIdsDistintos() {
        assertThat(sinOpciones.opciones()).isEmpty();
        assertThat(otro.opciones()).containsExactly("x");
        assertThat(sinOpciones.id()).isNotNull().isNotEqualTo(otro.id());
    }

    private void entoncesElLectorRecibeElBloqueConSusCamposSinLasClaves() {
        assertThat(conocido.id()).isEqualTo(bloque.id());
        assertThat(conocido.nombre()).isEqualTo("Ventas");
        assertThat(conocido.campos()).extracting("nombre", "tipo", "opciones")
                .containsExactly(tuple("Monto", TipoCampo.NUMERO, List.of()),
                        tuple("Estado", TipoCampo.OPCION, List.of("Pagó", "Debe")));
    }

    private void entoncesLaPropuestaLlevaElIdDeCadaCampo() {
        assertThat(lectura.id()).isEqualTo(bloque.id());
        assertThat(lectura.nombre()).isEqualTo("Ventas");
        assertThat(lectura.campos()).extracting(CampoConocido::id)
                .containsExactly(bloque.campos().get(0).id(), bloque.campos().get(1).id());
        assertThat(lectura.campos()).extracting(CampoConocido::nombre).containsExactly("Monto", "Estado");
    }

    private void entoncesCadaIdDeCampoApuntaASuClave() {
        assertThat(claves).containsEntry(bloque.campos().get(0).id(), "importe")
                .containsEntry(bloque.campos().get(1).id(), "estado").hasSize(2);
    }
}
