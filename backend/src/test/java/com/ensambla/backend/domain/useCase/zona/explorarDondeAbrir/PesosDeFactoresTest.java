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
    void baseShouldCoverEveryFactorWhenTradeIsSupported(RubroNegocio rubro) {
        whenSePidenLosPesosBaseDe(rubro);

        thenTodosLosFactoresTienenUnPesoPositivo();
    }

    @Test
    @DisplayName("Al almacén le pesa más la densidad de vecinos")
    void baseShouldWeighNeighborsMostWhenTradeIsGrocery() {
        whenSePidenLosPesosBaseDe(RubroNegocio.ALMACEN);

        thenElPesoDe(FactorDondeAbrir.DENSIDAD, .22);
    }

    @Test
    @DisplayName("A la papelería le pesan más las escuelas")
    void baseShouldWeighSchoolsMostWhenTradeIsStationery() {
        whenSePidenLosPesosBaseDe(RubroNegocio.PAPELERIA);

        thenElPesoDe(FactorDondeAbrir.ESCUELAS, .24);
    }

    @Test
    @DisplayName("Al kiosco le pesa más la actividad y el acceso")
    void baseShouldWeighActivityMostWhenTradeIsKiosk() {
        whenSePidenLosPesosBaseDe(RubroNegocio.KIOSCO);

        thenElPesoDe(FactorDondeAbrir.ACTIVIDAD_ACCESO, .24);
    }

    @Test
    @DisplayName("Los vecinos pesan más la densidad")
    void ajustarPorPublicoShouldBoostDensityWhenAudienceIsNeighbors() {
        givenTodosLosFactoresConPesoUno();

        whenSeAjustaPara(PublicoDondeAbrir.VECINOS);

        thenElPesoDe(FactorDondeAbrir.DENSIDAD, 1.6d);
        thenElPesoDe(FactorDondeAbrir.ESCUELAS, 1d);
    }

    @Test
    @DisplayName("Los estudiantes pesan más las escuelas y los parques")
    void ajustarPorPublicoShouldBoostSchoolsAndParksWhenAudienceIsStudents() {
        givenTodosLosFactoresConPesoUno();

        whenSeAjustaPara(PublicoDondeAbrir.ESTUDIANTES);

        thenElPesoDe(FactorDondeAbrir.ESCUELAS, 1.8d);
        thenElPesoDe(FactorDondeAbrir.PARQUES, 1.2d);
    }

    @Test
    @DisplayName("Las oficinas pesan más las oficinas y el transporte")
    void ajustarPorPublicoShouldBoostOfficesAndTransportWhenAudienceIsOffices() {
        givenTodosLosFactoresConPesoUno();

        whenSeAjustaPara(PublicoDondeAbrir.OFICINAS);

        thenElPesoDe(FactorDondeAbrir.OFICINAS, 1.8d);
        thenElPesoDe(FactorDondeAbrir.TRANSPORTE, 1.2d);
    }

    @Test
    @DisplayName("La importancia 0 anula el peso del factor")
    void multiplicadorDeImportanciaShouldBeZeroWhenLevelIsZero() {
        whenSePideElMultiplicadorDelNivel(0);

        thenElMultiplicadorEs(0d);
    }

    @Test
    @DisplayName("La importancia 1 deja el peso como está")
    void multiplicadorDeImportanciaShouldBeOneWhenLevelIsOne() {
        whenSePideElMultiplicadorDelNivel(1);

        thenElMultiplicadorEs(1d);
    }

    @Test
    @DisplayName("La importancia 2 pesa más del doble")
    void multiplicadorDeImportanciaShouldBeTwoPointTwoWhenLevelIsTwo() {
        whenSePideElMultiplicadorDelNivel(2);

        thenElMultiplicadorEs(2.2d);
    }

    @Test
    @DisplayName("Una importancia desconocida deja el peso como está")
    void multiplicadorDeImportanciaShouldBeOneWhenLevelIsUnknown() {
        whenSePideElMultiplicadorDelNivel(7);

        thenElMultiplicadorEs(1d);
    }

    private void givenTodosLosFactoresConPesoUno() {
        for (FactorDondeAbrir factor : FactorDondeAbrir.values()) {
            pesos.put(factor, 1d);
        }
    }

    private void whenSePidenLosPesosBaseDe(RubroNegocio rubro) {
        pesos = PesosDeFactores.base(rubro);
    }

    private void whenSeAjustaPara(PublicoDondeAbrir publico) {
        PesosDeFactores.ajustarPorPublico(pesos, publico);
    }

    private void whenSePideElMultiplicadorDelNivel(int nivel) {
        multiplicador = PesosDeFactores.multiplicadorDeImportancia(nivel);
    }

    private void thenTodosLosFactoresTienenUnPesoPositivo() {
        assertThat(pesos).hasSize(FactorDondeAbrir.values().length);
        assertThat(pesos.values()).allMatch(peso -> peso > 0d);
    }

    private void thenElPesoDe(FactorDondeAbrir factor, double peso) {
        assertThat(pesos).containsEntry(factor, peso);
    }

    private void thenElMultiplicadorEs(double esperado) {
        assertThat(multiplicador).isEqualTo(esperado);
    }
}
