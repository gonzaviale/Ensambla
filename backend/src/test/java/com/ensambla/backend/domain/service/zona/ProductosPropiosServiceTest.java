package com.ensambla.backend.domain.service.zona;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductosPropiosServiceTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId NOMBRE_ID = CampoId.de("f0000000-0000-4000-8000-000000000001");

    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final ProductosPropiosService service = new ProductosPropiosService(
            proyectoRepository, new DatosDeProyectoService(registroRepository));

    @Test
    @DisplayName("Devuelve los nombres del inventario sin las filas de ejemplo ni las sin nombre")
    void nombresDelInventarioShouldReturnRealNamedProducts() {
        Proyecto proyecto = givenProyectoConInventario(
                registro("Yerba Playadito 1 kg", false),
                registro("Fideos de ejemplo", true),
                registro("   ", false),
                Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID)
                        .valores(Map.of()).build());
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(proyecto));
        when(proyectoRepository.findByIdAndNegocioId(proyecto.getId(), NEGOCIO_ID)).thenReturn(Optional.of(proyecto));

        List<String> result = service.nombresDelInventario(NEGOCIO_ID);

        assertThat(result).containsExactly("Yerba Playadito 1 kg");
    }

    @Test
    @DisplayName("Si el proyecto ya no existe no aporta nombres")
    void nombresDelInventarioShouldSkipMissingProjects() {
        Proyecto proyecto = givenProyectoConInventario();
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(proyecto));
        when(proyectoRepository.findByIdAndNegocioId(proyecto.getId(), NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThat(service.nombresDelInventario(NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Un negocio sin proyectos no tiene nombres")
    void nombresDelInventarioShouldBeEmptyWithoutProjects() {
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());

        assertThat(service.nombresDelInventario(NEGOCIO_ID)).isEmpty();
    }

    // --- arrange ---
    private Proyecto givenProyectoConInventario(Registro... registros) {
        when(registroRepository.findAllByBloqueIdAndNegocioId(INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(List.of(registros));
        return Proyecto.builder().id(ProyectoId.nuevo()).nombre("Almacén").bloques(List.of(
                Bloque.builder().id(INVENTARIO_ID).negocioId(NEGOCIO_ID).tipo(TipoBloque.INVENTARIO)
                        .campos(List.of(Campo.builder().id(NOMBRE_ID).clave("name").build())).build())).build();
    }

    private static Registro registro(String nombre, boolean esEjemplo) {
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID)
                .esEjemplo(esEjemplo).valores(Map.of(NOMBRE_ID.toString(), nombre)).build();
    }
}
