package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CodigosInventarioTest {

    private static final CampoId CAMPO_ID = CampoId.nuevo();
    private static final String NO_ES_UN_CODIGO = "no es un código";

    private Bloque bloque;
    private List<Registro> registros;
    private Throwable rechazo;

    @Test
    @DisplayName("Rechaza GTIN equivalentes en dos productos del mismo inventario")
    void rechazaGtinEquivalentesEnDosProductosDelMismoInventario() {
        dadoUnInventarioConCodigoDeTipo(TipoCampo.TEXTO);
        dadoProductosConLosCodigos("77903754", "00000077903754");

        cuandoSeValidanLosCodigos();

        entoncesSeRechazaPorDuplicado();
    }

    @Test
    @DisplayName("Permite productos sin código y códigos diferentes sin modificar su stock")
    void permiteProductosSinCodigoYCodigosDiferentesSinModificarSuStock() {
        dadoUnInventarioConCodigoDeTipo(TipoCampo.TEXTO);
        dadoProductosSinCodigoYConCodigosDiferentes();

        cuandoSeValidanLosCodigos();

        entoncesNoSeRechazaNada();
    }

    @Test
    @DisplayName("Rechaza un campo numérico para el código porque elimina los ceros iniciales")
    void rechazaUnCampoNumericoParaElCodigoPorqueEliminaLosCerosIniciales() {
        dadoUnInventarioConCodigoDeTipo(TipoCampo.NUMERO);
        dadoProductosConLosCodigos();

        cuandoSeValidanLosCodigos();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza un valor numérico aunque el campo de código sea texto")
    void rechazaUnValorNumericoAunqueElCampoDeCodigoSeaTexto() {
        dadoUnInventarioConCodigoDeTipo(TipoCampo.TEXTO);
        registros = List.of(unRegistro(77903754));

        cuandoSeValidanLosCodigos();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("No interpreta como códigos los datos de otras tablas")
    void noInterpretaComoCodigosLosDatosDeOtrasTablas() {
        dadoUnInventarioConCodigoDeTipo(TipoCampo.TEXTO);
        bloque.setTipo(TipoBloque.VENTAS);
        dadoProductosConLosCodigos(NO_ES_UN_CODIGO);

        cuandoSeValidanLosCodigos();

        entoncesNoSeRechazaNada();
    }

    @Test
    @DisplayName("No interpreta como códigos los datos de otras columnas")
    void noInterpretaComoCodigosLosDatosDeOtrasColumnas() {
        dadoUnInventarioConCodigoDeTipo(TipoCampo.TEXTO);
        bloque.getCampos().getFirst().setClave("name");
        dadoProductosConLosCodigos(NO_ES_UN_CODIGO);

        cuandoSeValidanLosCodigos();

        entoncesNoSeRechazaNada();
    }

    // --- dado ---

    private void dadoUnInventarioConCodigoDeTipo(TipoCampo tipo) {
        bloque = Bloque.builder().tipo(TipoBloque.INVENTARIO).campos(List.of(
                Campo.builder().id(CAMPO_ID).clave("barcode").obligatorio(false).tipo(tipo).build())).build();
    }

    private void dadoProductosConLosCodigos(String... codigos) {
        registros = Arrays.stream(codigos).map(this::unRegistro).toList();
    }

    private void dadoProductosSinCodigoYConCodigosDiferentes() {
        registros = List.of(Registro.builder().valores(Map.of()).build(), unRegistro(""),
                unRegistro("77903754"), unRegistro("77905741"));
    }

    // --- cuando ---

    private void cuandoSeValidanLosCodigos() {
        rechazo = catchThrowable(() -> CodigosInventario.validar(bloque, registros));
    }

    // --- entonces ---

    private void entoncesSeRechazaPorDuplicado() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesNoSeRechazaNada() {
        assertThat(rechazo).isNull();
    }

    // --- datos ---

    private Registro unRegistro(Object codigo) {
        return Registro.builder().valores(Map.of(CAMPO_ID.toString(), codigo)).build();
    }
}
