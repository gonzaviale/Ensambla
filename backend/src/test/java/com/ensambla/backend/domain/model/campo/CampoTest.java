package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.CamposDePrueba.unCampoDeTexto;
import static com.ensambla.backend.support.CamposDePrueba.unCampoNumerico;
import static com.ensambla.backend.support.CamposDePrueba.unaFormulaQueUsa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CampoTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private Campo campoResultante;
    private Throwable rechazo;

    @Test
    @DisplayName("Un campo marcado como obligatorio es obligatorio")
    void unCampoMarcadoComoObligatorioEsObligatorio() {
        Campo campo = unCampoObligatorio(true);

        entoncesElCampoEsObligatorio(campo);
    }

    @Test
    @DisplayName("Un campo sin marcar como obligatorio no es obligatorio")
    void unCampoSinMarcarNoEsObligatorio() {
        Campo campo = unCampoObligatorio(false);

        entoncesElCampoNoEsObligatorio(campo);
    }

    @Test
    @DisplayName("Un campo sin dato de obligatoriedad no es obligatorio")
    void unCampoSinDatoDeObligatorioNoEsObligatorio() {
        Campo campo = unCampoObligatorio(null);

        entoncesElCampoNoEsObligatorio(campo);
    }

    @Test
    @DisplayName("Si no se indica el tipo, el campo es de texto")
    void sinIndicarTipoElCampoEsDeTexto() {
        cuandoSeArmaUnCampoSinIndicarTipo();

        entoncesElCampoEsDeTexto();
    }

    @Test
    @DisplayName("Un campo de texto no tiene datos de otros tipos")
    void unCampoDeTextoNoTieneDatosDeOtrosTipos() {
        Campo texto = unCampoDeTexto("Producto");

        entoncesNoTieneDatosDeOtrosTipos(texto);
    }

    @Test
    @DisplayName("Un campo nuevo queda ubicado en el bloque con las dos fechas iguales")
    void unCampoNuevoQuedaEnElLugarDelBloqueConLasDosFechasIguales() {
        Campo campo = unCampoSinUbicar();

        cuandoSeUbicaEnElBloque(campo);

        entoncesElCampoQuedaUbicadoEnElBloqueEnLaPosicionDos();
    }

    @Test
    @DisplayName("El campo que reemplaza a otro conserva su id, su lugar y su fecha de creación")
    void elCampoQueReemplazaAOtroConservaSuIdSuLugarYSuFechaDeCreacion() {
        Campo anterior = unCampoGuardado();

        cuandoUnCampoNuevoReemplazaA(anterior);

        entoncesElReemplazoConservaLaIdentidadDelAnterior();
    }

    @Test
    @DisplayName("Un campo que ninguna fórmula usa se puede eliminar")
    void unCampoQueNingunaFormulaUsaSePuedeEliminar() {
        Campo precio = unCampoNumerico("Precio");

        cuandoSeValidaEliminarlo(precio, precio);

        entoncesNoSeRechazaNada();
    }

    @Test
    @DisplayName("Un campo del que depende una fórmula no se puede eliminar")
    void unCampoDelQueDependeUnaFormulaNoSePuedeEliminar() {
        Campo precio = unCampoNumerico("Precio");

        cuandoSeValidaEliminarlo(precio, precio, unaFormulaQueUsa(precio, "Total"));

        entoncesSeRechazaPorque("No se puede eliminar «Precio»: la fórmula «Total» lo usa");
    }

    @Test
    @DisplayName("Un campo de texto no puede cambiar de tipo si una fórmula lo usa")
    void unCampoDeTextoNoPuedeCambiarDeTipoSiUnaFormulaLoUsa() {
        Campo precio = unCampoDeTexto("Precio");

        cuandoSeValidaCambiarleElTipo(precio, precio, unaFormulaQueUsa(precio, "Total"));

        entoncesSeRechazaPorque("No se puede cambiar el tipo de «Precio»: la fórmula «Total» lo usa");
    }

    private Campo unCampoObligatorio(Boolean obligatorio) {
        return Campo.builder().obligatorio(obligatorio).build();
    }

    private Campo unCampoSinUbicar() {
        return Campo.builder().nombre("Talle").build();
    }

    private Campo unCampoGuardado() {
        return Campo.builder()
                .id(CAMPO_ID)
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .posicion((short) 3)
                .clave("price")
                .minimo(BigDecimal.TEN)
                .creadoEn(AHORA)
                .build();
    }

    private void cuandoSeArmaUnCampoSinIndicarTipo() {
        campoResultante = Campo.builder().build();
    }

    private void cuandoSeUbicaEnElBloque(Campo campo) {
        campoResultante = campo.ubicadoEn(NEGOCIO_ID, BLOQUE_ID, (short) 2, AHORA);
    }

    private void cuandoUnCampoNuevoReemplazaA(Campo anterior) {
        Campo nuevo = Campo.builder().nombre("Precio").tipo(TipoCampo.NUMERO).build();
        campoResultante = nuevo.reemplazandoA(anterior, AHORA.plusHours(1));
    }

    private void cuandoSeValidaEliminarlo(Campo campo, Campo... camposDelBloque) {
        rechazo = catchThrowable(() -> campo.validarSinFormulasQueLoUsen(List.of(camposDelBloque), "eliminar"));
    }

    private void cuandoSeValidaCambiarleElTipo(Campo campo, Campo... camposDelBloque) {
        rechazo = catchThrowable(() -> campo.validarCambioDeTipo(List.of(camposDelBloque)));
    }

    private void entoncesElCampoEsObligatorio(Campo campo) {
        assertThat(campo.esObligatorio()).isTrue();
    }

    private void entoncesElCampoNoEsObligatorio(Campo campo) {
        assertThat(campo.esObligatorio()).isFalse();
    }

    private void entoncesElCampoEsDeTexto() {
        assertThat(campoResultante).isInstanceOf(CampoTexto.class);
    }

    private void entoncesNoTieneDatosDeOtrosTipos(Campo texto) {
        assertThat(texto.esNumerico()).isFalse();
        assertThat(texto.esRelacion()).isFalse();
        assertThat(texto.esFormula()).isFalse();
        assertThat(texto.getOpciones()).isEmpty();
        assertThat(texto.getBloqueRelacionadoId()).isNull();
        assertThat(texto.getFormula()).isNull();
    }

    private void entoncesElCampoQuedaUbicadoEnElBloqueEnLaPosicionDos() {
        assertThat(campoResultante.getId()).isNull();
        assertThat(campoResultante.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(campoResultante.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(campoResultante.getPosicion()).isEqualTo((short) 2);
        assertThat(campoResultante.getCreadoEn()).isEqualTo(AHORA);
        assertThat(campoResultante.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElReemplazoConservaLaIdentidadDelAnterior() {
        assertThat(campoResultante.getNombre()).isEqualTo("Precio");
        assertThat(campoResultante.getTipo()).isEqualTo(TipoCampo.NUMERO);
        assertThat(campoResultante.getId()).isEqualTo(CAMPO_ID);
        assertThat(campoResultante.getBloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(campoResultante.getPosicion()).isEqualTo((short) 3);
        assertThat(campoResultante.getClave()).isEqualTo("price");
        assertThat(campoResultante.getMinimo()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(campoResultante.getCreadoEn()).isEqualTo(AHORA);
        assertThat(campoResultante.getActualizadoEn()).isEqualTo(AHORA.plusHours(1));
    }

    private void entoncesNoSeRechazaNada() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }
}
