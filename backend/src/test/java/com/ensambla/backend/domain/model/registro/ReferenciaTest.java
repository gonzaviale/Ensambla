package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.zona.ZonaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenciaTest {

    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final ZonaId ZONA_ID = ZonaId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId ALIAS_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");

    private Referencia referencia;
    private List<String> etiquetas;

    @Test
    @DisplayName("La referencia lleva el id del registro y el valor del primer campo de texto")
    void laReferenciaLlevaElIdDelRegistroYElValorDelPrimerCampoDeTexto() {
        List<Campo> campos = List.of(
                unCampo(PRECIO_ID.valor(), TipoCampo.NUMERO, 0), unCampo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 1));

        cuandoSeArmaLaReferencia(
                unRegistro(Map.of(PRECIO_ID.toString(), 10, NOMBRE_ID.toString(), "Ana")), campos);

        entoncesLaReferenciaEsDelRegistroConLaEtiqueta("Ana");
    }

    @Test
    @DisplayName("Un campo de opción también sirve de etiqueta")
    void unCampoDeOpcionTambienSirveDeEtiqueta() {
        List<Campo> campos = List.of(
                unCampo(PRECIO_ID.valor(), TipoCampo.NUMERO, 0), unCampo(ZONA_ID.valor(), TipoCampo.OPCION, 1));

        cuandoSeArmaLaReferencia(unRegistro(Map.of(ZONA_ID.toString(), "Norte")), campos);

        entoncesLaEtiquetaEs("Norte");
    }

    @Test
    @DisplayName("Elige el campo descriptivo de menor posición aunque la lista venga desordenada")
    void eligeElCampoDescriptivoDeMenorPosicionAunqueLaListaVengaDesordenada() {
        List<Campo> campos = List.of(
                unCampo(ZONA_ID.valor(), TipoCampo.OPCION, 3), unCampo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 1));

        cuandoSeArmaLaReferencia(
                unRegistro(Map.of(ZONA_ID.toString(), "Norte", NOMBRE_ID.toString(), "Ana")), campos);

        entoncesLaEtiquetaEs("Ana");
    }

    @Test
    @DisplayName("Con la misma posición desempata el id del campo")
    void conLaMismaPosicionDesempataElIdDelCampo() {
        List<Campo> campos = List.of(
                unCampo(ALIAS_ID.valor(), TipoCampo.TEXTO, 0), unCampo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 0));

        cuandoSeArmaLaReferencia(
                unRegistro(Map.of(ALIAS_ID.toString(), "Anita", NOMBRE_ID.toString(), "Ana")), campos);

        entoncesLaEtiquetaEs("Ana");
    }

    @Test
    @DisplayName("Sin campos de texto ni de opción usa el primer campo, convertido a texto")
    void sinCamposDeTextoNiDeOpcionUsaElPrimerCampoConvertidoATexto() {
        List<Campo> campos = List.of(
                unCampo(ZONA_ID.valor(), TipoCampo.SI_NO, 1), unCampo(PRECIO_ID.valor(), TipoCampo.NUMERO, 0));

        cuandoSeArmaLaReferencia(unRegistro(Map.of(PRECIO_ID.toString(), 12.5)), campos);

        entoncesLaEtiquetaEs("12.5");
    }

    @Test
    @DisplayName("Si la fila no cargó el dato de la etiqueta queda \"Sin nombre\"")
    void siLaFilaNoCargoElDatoDeLaEtiquetaQuedaSinNombre() {
        cuandoSeArmaLaReferencia(unRegistro(Map.of()), List.of(unCampo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 0)));

        entoncesLaEtiquetaEs(Referencia.SIN_NOMBRE);
    }

    @Test
    @DisplayName("Un dato en blanco cuenta como faltante")
    void unDatoEnBlancoCuentaComoFaltante() {
        cuandoSeArmaLaReferencia(unRegistro(Map.of(NOMBRE_ID.toString(), "  ")),
                List.of(unCampo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 0)));

        entoncesLaEtiquetaEs(Referencia.SIN_NOMBRE);
    }

    @Test
    @DisplayName("Un bloque sin campos da \"Sin nombre\"")
    void unBloqueSinCamposDaSinNombre() {
        cuandoSeArmaLaReferencia(unRegistro(Map.of(NOMBRE_ID.toString(), "Ana")), List.of());

        entoncesLaEtiquetaEs("Sin nombre");
    }

    @Test
    @DisplayName("El etiquetador elige el campo una vez y lo aplica a cada registro")
    void elEtiquetadorEligeElCampoUnaVezYLoAplicaACadaRegistro() {
        Function<Registro, Referencia> etiquetador = dadoUnEtiquetadorPorNombre();

        cuandoSeEtiquetanAnaYBeto(etiquetador);

        entoncesLasEtiquetasSon("Ana", "Beto");
    }

    // --- dado ---
    private Function<Registro, Referencia> dadoUnEtiquetadorPorNombre() {
        return Referencia.etiquetador(List.of(unCampo(NOMBRE_ID.valor(), TipoCampo.TEXTO, 0)));
    }

    private Campo unCampo(UUID id, TipoCampo tipo, int posicion) {
        return Campo.builder()
                .id(CampoId.de(id))
                .tipo(tipo)
                .posicion((short) posicion)
                .build();
    }

    private Registro unRegistro(Map<String, Object> valores) {
        return Registro.builder()
                .id(REGISTRO_ID)
                .valores(valores)
                .build();
    }

    // --- cuando ---
    private void cuandoSeArmaLaReferencia(Registro registro, List<Campo> campos) {
        referencia = Referencia.de(registro, campos);
    }

    private void cuandoSeEtiquetanAnaYBeto(Function<Registro, Referencia> etiquetador) {
        etiquetas = List.of(
                        unRegistro(Map.of(NOMBRE_ID.toString(), "Ana")),
                        unRegistro(Map.of(NOMBRE_ID.toString(), "Beto")))
                .stream()
                .map(etiquetador)
                .map(Referencia::etiqueta)
                .toList();
    }

    // --- entonces ---
    private void entoncesLaReferenciaEsDelRegistroConLaEtiqueta(String etiqueta) {
        assertThat(referencia.id()).isEqualTo(REGISTRO_ID);
        assertThat(referencia.etiqueta()).isEqualTo(etiqueta);
    }

    private void entoncesLaEtiquetaEs(String esperada) {
        assertThat(referencia.etiqueta()).isEqualTo(esperada);
    }

    private void entoncesLasEtiquetasSon(String primera, String segunda) {
        assertThat(etiquetas).containsExactly(primera, segunda);
    }
}
