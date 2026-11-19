package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ConsultaRegistrosTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId FORMULA_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final List<Campo> CAMPOS = List.of(Campo.builder().id(CAMPO_ID).build());

    private ConsultaRegistros consulta;
    private Throwable rechazo;

    @Test
    @DisplayName("Sin parámetros pide la primera página de 20 por fecha de carga descendente")
    void sinParametrosPideLaPrimeraPaginaDeVeintePorFechaDeCargaDescendente() {
        cuandoSeArma(null, null, null, null, null);

        entoncesPideLaPrimeraPaginaDeVeintePorFechaDeCargaDescendente();
    }

    @Test
    @DisplayName("Ignora el texto en blanco y quita los espacios del texto a buscar")
    void ignoraElTextoEnBlancoYQuitaLosEspaciosDelTextoABuscar() {
        cuandoSeArma("   ", null, null, null, null);
        entoncesNoBuscaTexto();

        cuandoSeArma("  harina ", null, null, null, null);
        entoncesBuscaElTexto("harina");
    }

    @Test
    @DisplayName("Ordena por fecha de carga cuando el orden es vacío o creadoEn")
    void ordenaPorFechaDeCargaCuandoElOrdenEsVacioOCreadoEn() {
        cuandoSeArma(null, "  ", null, null, null);
        entoncesOrdenaPorFechaDeCarga();

        cuandoSeArma(null, "creadoEn", null, null, null);
        entoncesOrdenaPorFechaDeCarga();
    }

    @Test
    @DisplayName("Ordena por un campo del bloque identificado por su id")
    void ordenaPorUnCampoDelBloqueIdentificadoPorSuId() {
        cuandoSeArma(null, CAMPO_ID.toString(), "asc", null, null);

        entoncesOrdenaAscendentePorElCampo(CAMPO_ID);
    }

    @Test
    @DisplayName("Rechaza ordenar por un campo que no es del bloque")
    void rechazaOrdenarPorUnCampoQueNoEsDelBloque() {
        cuandoSeIntentaArmar(null, UUID.randomUUID().toString(), null, null, null);

        entoncesSeRechazaElOrdenPorEseCampo();
    }

    @Test
    @DisplayName("Rechaza una dirección distinta de asc y desc")
    void rechazaUnaDireccionDistintaDeAscYDesc() {
        cuandoSeIntentaArmar(null, null, "arriba", null, null);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Corrige una página negativa a la primera")
    void corrigeUnaPaginaNegativaALaPrimera() {
        cuandoSeArma(null, null, null, -3, null);
        entoncesPideLaPagina(0);

        cuandoSeArma(null, null, null, 4, null);
        entoncesPideLaPagina(4);
    }

    @Test
    @DisplayName("Acota el tamaño de página entre 1 y 100")
    void acotaElTamanioDePaginaEntreUnoYCien() {
        cuandoSeArma(null, null, null, null, 0);
        entoncesPideUnTamanioDe(1);

        cuandoSeArma(null, null, null, null, 500);
        entoncesPideUnTamanioDe(100);

        cuandoSeArma(null, null, null, null, 50);
        entoncesPideUnTamanioDe(50);
    }

    @Test
    @DisplayName("Rechaza ordenar por un campo fórmula, que no se guarda")
    void rechazaOrdenarPorUnCampoFormulaQueNoSeGuarda() {
        cuandoSeIntentaOrdenarPorUnCampoFormula();

        entoncesSeRechazaElOrdenPorEseCampo();
    }

    // --- cuando ---
    private void cuandoSeArma(String texto, String orden, String direccion, Integer pagina, Integer tamanio) {
        consulta = ConsultaRegistros.armar(NEGOCIO_ID, BLOQUE_ID, CAMPOS, texto, orden, direccion, pagina, tamanio);
    }

    private void cuandoSeIntentaArmar(String texto, String orden, String direccion, Integer pagina,
                                      Integer tamanio) {
        rechazo = catchThrowable(() -> cuandoSeArma(texto, orden, direccion, pagina, tamanio));
    }

    private void cuandoSeIntentaOrdenarPorUnCampoFormula() {
        List<Campo> campos = List.of(Campo.builder().id(FORMULA_ID).tipo(TipoCampo.FORMULA).build());
        rechazo = catchThrowable(() -> ConsultaRegistros.armar(
                NEGOCIO_ID, BLOQUE_ID, campos, null, FORMULA_ID.toString(), null, null, null));
    }

    // --- entonces ---
    private void entoncesPideLaPrimeraPaginaDeVeintePorFechaDeCargaDescendente() {
        assertThat(consulta.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(consulta.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(consulta.texto()).isNull();
        assertThat(consulta.campoOrdenId()).isNull();
        assertThat(consulta.direccion()).isEqualTo(DireccionOrden.DESC);
        assertThat(consulta.pagina()).isZero();
        assertThat(consulta.tamanio()).isEqualTo(20);
    }

    private void entoncesNoBuscaTexto() {
        assertThat(consulta.texto()).isNull();
    }

    private void entoncesBuscaElTexto(String esperado) {
        assertThat(consulta.texto()).isEqualTo(esperado);
    }

    private void entoncesOrdenaPorFechaDeCarga() {
        assertThat(consulta.campoOrdenId()).isNull();
    }

    private void entoncesOrdenaAscendentePorElCampo(CampoId campoId) {
        assertThat(consulta.campoOrdenId()).isEqualTo(campoId);
        assertThat(consulta.direccion()).isEqualTo(DireccionOrden.ASC);
    }

    private void entoncesPideLaPagina(int esperada) {
        assertThat(consulta.pagina()).isEqualTo(esperada);
    }

    private void entoncesPideUnTamanioDe(int esperado) {
        assertThat(consulta.tamanio()).isEqualTo(esperado);
    }

    private void entoncesSeRechazaElOrdenPorEseCampo() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede ordenar por ese campo");
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }
}
