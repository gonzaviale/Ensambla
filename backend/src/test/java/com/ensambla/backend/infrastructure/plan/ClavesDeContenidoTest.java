package com.ensambla.backend.infrastructure.plan;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClavesDeContenidoTest {

    private String clave;
    private Map<String, String> origenes;

    @Test
    @DisplayName("Un aviso se guarda con el prefijo del servidor")
    void unAvisoSeGuardaConElPrefijoDelServidor() {
        cuandoSeGuardaLaClaveDelAviso("a1");

        entoncesLaClaveEs("servidor:a1");
    }

    @Test
    @DisplayName("El contenido de zona se guarda con su clave tal cual")
    void elContenidoDeZonaSeGuardaConSuClaveTalCual() {
        cuandoSeGuardaLaClaveDeZona("yerba");

        entoncesLaClaveEs("yerba");
    }

    @Test
    @DisplayName("Al leer un aviso se le quita el prefijo del servidor")
    void alLeerUnAvisoSeLeQuitaElPrefijoDelServidor() {
        cuandoSeLeeLaClaveDelAviso("servidor:a1");

        entoncesLaClaveEs("a1");
    }

    @Test
    @DisplayName("Una clave guardada sin prefijo se lee igual")
    void unaClaveGuardadaSinPrefijoSeLeeIgual() {
        cuandoSeLeeLaClaveDelAviso("demo:1");

        entoncesLaClaveEs("demo:1");
    }

    @Test
    @DisplayName("Una clave de zona se lee igual aunque no tenga prefijo")
    void unaClaveDeZonaSeLeeIgualAunqueNoTengaPrefijo() {
        cuandoSeLeeLaClaveDeZona("yerba");

        entoncesLaClaveEs("yerba");
    }

    @Test
    @DisplayName("Cada clave del pedido se asocia a la clave del dominio en el mismo orden")
    void cadaClaveDelPedidoSeAsociaALaClaveDelDominioEnElMismoOrden() {
        cuandoSePidenLosAvisos("servidor:a1", "demo:1");

        entoncesLosOrigenesSonEnOrden("a1", "servidor:a1", "demo:1", "demo:1");
    }

    private void cuandoSeGuardaLaClaveDelAviso(String claveDelDominio) {
        clave = ClavesDeContenido.guardada(TipoContenidoPlan.AVISOS, claveDelDominio);
    }

    private void cuandoSeGuardaLaClaveDeZona(String claveDelDominio) {
        clave = ClavesDeContenido.guardada(TipoContenidoPlan.ZONA, claveDelDominio);
    }

    private void cuandoSeLeeLaClaveDelAviso(String claveGuardada) {
        clave = ClavesDeContenido.delDominio(TipoContenidoPlan.AVISOS, claveGuardada);
    }

    private void cuandoSeLeeLaClaveDeZona(String claveGuardada) {
        clave = ClavesDeContenido.delDominio(TipoContenidoPlan.ZONA, claveGuardada);
    }

    private void cuandoSePidenLosAvisos(String... clavesPedidas) {
        origenes = ClavesDeContenido.origenesDelPedido(TipoContenidoPlan.AVISOS, List.of(clavesPedidas));
    }

    private void entoncesLaClaveEs(String esperada) {
        assertThat(clave).isEqualTo(esperada);
    }

    private void entoncesLosOrigenesSonEnOrden(String dominio1, String pedida1, String dominio2, String pedida2) {
        assertThat(origenes).containsExactly(Map.entry(dominio1, pedida1), Map.entry(dominio2, pedida2));
    }
}
