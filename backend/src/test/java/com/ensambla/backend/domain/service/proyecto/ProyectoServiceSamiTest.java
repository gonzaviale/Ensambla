package com.ensambla.backend.domain.service.proyecto;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceSamiTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    private ProyectoService proyectoService;

    @BeforeEach
    void setUp() {
        proyectoService = new ProyectoService(proyectoRepository);
    }

    @Test
    @DisplayName("Guarda en una sola operación la plantilla con las columnas extra y el nombre elegido")
    void crearDesdePlantillaShouldPersistTemplateWithExtraColumnsAndName() {
        when(proyectoRepository.saveConEstructura(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Proyecto result = proyectoService.crearDesdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(),
                "  Velas de Ana ", List.of(columna("Saldo pendiente")), AHORA);

        assertThat(result.getNombre()).isEqualTo("Velas de Ana");
        assertThat(result.getBloques().getFirst().getCampos()).extracting(Campo::getNombre)
                .containsExactly("Nombre", "Rubro", "Saldo pendiente");
        verify(proyectoRepository).saveConEstructura(result);
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin nombre o con el nombre en blanco, el proyecto se llama como la plantilla")
    void crearDesdePlantillaShouldKeepTemplateNameWithoutCustomName() {
        when(proyectoRepository.saveConEstructura(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Proyecto sinNombre = proyectoService.crearDesdePlantilla(
                NEGOCIO_ID, ProyectoTestData.plantilla(), null, List.of(), AHORA);
        Proyecto enBlanco = proyectoService.crearDesdePlantilla(
                NEGOCIO_ID, ProyectoTestData.plantilla(), "   ", List.of(), AHORA);

        assertThat(sinNombre.getNombre()).isEqualTo("Inventario");
        assertThat(enBlanco.getNombre()).isEqualTo("Inventario");
    }

    @Test
    @DisplayName("Si una columna falla no se guarda nada")
    void crearDesdePlantillaShouldNotPersistWhenAColumnFails() {
        List<ConfiguracionCampo> columnas = List.of(columna("Saldo pendiente"), columna(" "));

        assertThatThrownBy(() -> proyectoService.crearDesdePlantilla(
                NEGOCIO_ID, ProyectoTestData.plantilla(), null, columnas, AHORA))
                .isInstanceOf(CampoInvalidoException.class);
        verify(proyectoRepository, never()).saveConEstructura(any());
    }

    private ConfiguracionCampo columna(String nombre) {
        return new ConfiguracionCampo(nombre, "NUMERO", false, null, null, null, null);
    }
}
