package com.ensambla.backend.domain.useCase.zona.explorarDondeAbrir;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PesosDeFactoresTest {

    private Map<FactorDondeAbrir, Double> pesos = new EnumMap<>(FactorDondeAbrir.class);
    private double multiplicador;

    @ParameterizedTest
    @EnumSource(value = RubroNegocio.class, names = {"ALMACEN", "PAPELERIA", "KIOSCO"})
    @DisplayName("Los pesos base de cada rubro cubren todos los factores con un peso positivo")
    void losPesosBaseDeCadaRubroCubrenTodosLosFactores(RubroNegocio rubro) {
        cuandoSePidenLosPesosBaseDe(rubro);

        entoncesTodosLosFactoresTienenUnPesoPositivo();
    }

    @Test
    @DisplayName("Al almacén le pesa más la densidad de vecinos")
    void alAlmacenLePesaMasLaDensidadDeVecinos() {
        cuandoSePidenLosPesosBaseDe(RubroNegocio.ALMACEN);

        entoncesElPesoDe(FactorDondeAbrir.DENSIDAD, .22);
    }

    @Test
    @DisplayName("A la papelería le pesan más las escuelas")
    void aLaPapeleriaLePesanMasLasEscuelas() {
        cuandoSePidenLosPesosBaseDe(RubroNegocio.PAPELERIA);

        entoncesElPesoDe(FactorDondeAbrir.ESCUELAS, .24);
    }

    @Test
    @DisplayName("Al kiosco le pesa más la actividad y el acceso")
    void alKioscoLePesaMasLaActividadYElAcceso() {
        cuandoSePidenLosPesosBaseDe(RubroNegocio.KIOSCO);

        entoncesElPesoDe(FactorDondeAbrir.ACTIVIDAD_ACCESO, .24);
    }

    @Test
    @DisplayName("Los vecinos pesan más la densidad")
    void losVecinosPesanMasLaDensidad() {
        dadoTodosLosFactoresConPesoUno();

        cuandoSeAjustaPara(PublicoDondeAbrir.VECINOS);

        entoncesElPesoDe(FactorDondeAbrir.DENSIDAD, 1.6d);
        entoncesElPesoDe(FactorDondeAbrir.ESCUELAS, 1d);
    }

    @Test
    @DisplayName("Los estudiantes pesan más las escuelas y los parques")
    void losEstudiantesPesanMasLasEscuelasYLosParques() {
        dadoTodosLosFactoresConPesoUno();

        cuandoSeAjustaPara(PublicoDondeAbrir.ESTUDIANTES);

        entoncesElPesoDe(FactorDondeAbrir.ESCUELAS, 1.8d);
        entoncesElPesoDe(FactorDondeAbrir.PARQUES, 1.2d);
    }

    @Test
    @DisplayName("Las oficinas pesan más las oficinas y el transporte")
    void lasOficinasPesanMasLasOficinasYElTransporte() {
        dadoTodosLosFactoresConPesoUno();

        cuandoSeAjustaPara(PublicoDondeAbrir.OFICINAS);

        entoncesElPesoDe(FactorDondeAbrir.OFICINAS, 1.8d);
        entoncesElPesoDe(FactorDondeAbrir.TRANSPORTE, 1.2d);
    }

    @Test
    @DisplayName("La importancia 0 anula el peso del factor")
    void laImportanciaCeroAnulaElPesoDelFactor() {
        cuandoSePideElMultiplicadorDelNivel(0);

        entoncesElMultiplicadorEs(0d);
    }

    @Test
    @DisplayName("La importancia 1 deja el peso como está")
    void laImportanciaUnoDejaElPesoComoEsta() {
        cuandoSePideElMultiplicadorDelNivel(1);

        entoncesElMultiplicadorEs(1d);
    }

    @Test
    @DisplayName("La importancia 2 pesa más del doble")
    void laImportanciaDosPesaMasDelDoble() {
        cuandoSePideElMultiplicadorDelNivel(2);

        entoncesElMultiplicadorEs(2.2d);
    }

    @Test
    @DisplayName("Una importancia desconocida deja el peso como está")
    void unaImportanciaDesconocidaDejaElPesoComoEsta() {
        cuandoSePideElMultiplicadorDelNivel(7);

        entoncesElMultiplicadorEs(1d);
    }

    private void dadoTodosLosFactoresConPesoUno() {
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            pesos.put(factor, 1d);
        }
    }

    private void cuandoSePidenLosPesosBaseDe(RubroNegocio rubro) {
        pesos = PesosDeFactores.base(rubro);
    }

    private void cuandoSeAjustaPara(PublicoDondeAbrir publico) {
        PesosDeFactores.ajustarPorPublico(pesos, publico);
    }

    private void cuandoSePideElMultiplicadorDelNivel(int nivel) {
        multiplicador = PesosDeFactores.multiplicadorDeImportancia(nivel);
    }

    private void entoncesTodosLosFactoresTienenUnPesoPositivo() {
        assertThat(pesos).hasSize(FactorDondeAbrir.values().length);
        assertThat(pesos.values()).allMatch(peso -> peso > 0d);
    }

    private void entoncesElPesoDe(FactorDondeAbrir factor, double peso) {
        assertThat(pesos).containsEntry(factor, peso);
    }

    private void entoncesElMultiplicadorEs(double esperado) {
        assertThat(multiplicador).isEqualTo(esperado);
    }
}
