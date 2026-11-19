package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlantillaJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PlantillaRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantillaRepositoryImplTest {

    private static final UUID PRIMER_ID = UUID.randomUUID();
    private static final UUID SEGUNDO_ID = UUID.randomUUID();

    @Mock
    private IPlantillaJpaRepository jpa;

    private PlantillaRepositoryImpl repository;

    @BeforeEach
    void dadoUnRepositorioDePlantillas() {
        repository = new PlantillaRepositoryImpl(jpa, new PlantillaRepositoryMapper());
    }

    private List<Plantilla> plantillas;
    private List<PlantillaProyecto> plantillasParaProyecto;

    @Test
    @DisplayName("Devuelve las plantillas activas mapeadas y en el orden de la consulta")
    void devuelveLasPlantillasActivasMapeadasYEnElOrdenDeLaConsulta() {
        dadoQueHayDosPlantillasActivas();

        cuandoSeListanLasPlantillasActivas();

        entoncesSeListanClientesYPedidosEnOrden();
    }

    @Test
    @DisplayName("Devuelve las plantillas activas con su estructura interpretada, en el orden de la consulta")
    void devuelveLasPlantillasActivasConSuEstructuraInterpretadaEnElOrdenDeLaConsulta() {
        dadoQueHayUnaPlantillaActivaDeVentasConUnCampoNumerico();

        cuandoSeListanLasPlantillasActivasParaProyecto();

        entoncesSeInterpretaLaEstructuraDeVentas();
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay plantillas activas")
    void devuelveUnaListaVaciaCuandoNoHayPlantillasActivas() {
        dadoQueNoHayPlantillasActivas();

        cuandoSeListanLasPlantillasActivas();

        entoncesNoSeListaNingunaPlantilla();
    }

    // --- dado ---
    private void dadoQueHayDosPlantillasActivas() {
        when(jpa.findByActivaTrueOrderByNombreAscIdAsc()).thenReturn(List.of(
                PlantillaEntity.builder().id(PRIMER_ID).nombre("Clientes").build(),
                PlantillaEntity.builder().id(SEGUNDO_ID).nombre("Pedidos").build()));
    }

    private void dadoQueHayUnaPlantillaActivaDeVentasConUnCampoNumerico() {
        when(jpa.findByActivaTrueOrderByNombreAscIdAsc()).thenReturn(List.of(PlantillaEntity.builder()
                .id(PRIMER_ID)
                .nombre("Ventas")
                .descripcion("Anotá lo que vendés")
                .estructura(Map.of("bloques", List.of(Map.of(
                        "nombre", "Ventas",
                        "campos", List.of(Map.of("nombre", "Monto", "tipo", "NUMERO"))))))
                .build()));
    }

    private void dadoQueNoHayPlantillasActivas() {
        when(jpa.findByActivaTrueOrderByNombreAscIdAsc()).thenReturn(List.of());
    }

    // --- cuando ---
    private void cuandoSeListanLasPlantillasActivas() {
        plantillas = repository.findAllActivas();
    }

    private void cuandoSeListanLasPlantillasActivasParaProyecto() {
        plantillasParaProyecto = repository.findAllParaProyectoActivas();
    }

    // --- entonces ---
    private void entoncesSeListanClientesYPedidosEnOrden() {
        assertThat(plantillas).extracting(Plantilla::getId)
                .containsExactly(PlantillaId.de(PRIMER_ID), PlantillaId.de(SEGUNDO_ID));
    }

    private void entoncesSeInterpretaLaEstructuraDeVentas() {
        assertThat(plantillasParaProyecto).extracting(PlantillaProyecto::id, PlantillaProyecto::nombre)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(PlantillaId.de(PRIMER_ID), "Ventas"));
        assertThat(plantillasParaProyecto.getFirst().estructura().bloques().getFirst().campos())
                .extracting("nombre", "tipo")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Monto", TipoCampo.NUMERO));
    }

    private void entoncesNoSeListaNingunaPlantilla() {
        assertThat(plantillas).isEmpty();
    }
}
