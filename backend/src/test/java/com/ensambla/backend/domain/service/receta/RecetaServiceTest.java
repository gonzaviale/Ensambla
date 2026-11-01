package com.ensambla.backend.domain.service.receta;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaServiceTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_INSUMOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId HARINA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");

    @Mock
    private CampoRepository campoRepository;

    @Mock
    private RegistroRepository registroRepository;

    @InjectMocks
    private RecetaService recetaService;

    @Test
    @DisplayName("Costear calcula el costo con los insumos y los campos de su tabla")
    void costearShouldPriceTheInsumos() {
        Receta receta = givenReceta(HARINA_ID);
        givenColumnaDelPrecio(TipoCampo.NUMERO);
        when(registroRepository.findAllByIdsAndBloque(List.of(HARINA_ID), BLOQUE_INSUMOS_ID, NEGOCIO_ID))
                .thenReturn(List.of(Registro.builder().id(HARINA_ID)
                        .valores(Map.of(PRECIO_ID.toString(), 10)).build()));
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_INSUMOS_ID, NEGOCIO_ID)).thenReturn(List.of());

        RecetaCosteada result = recetaService.costear(receta, NEGOCIO_ID);

        assertThat(result.costo()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(result.bloqueInsumosId()).isEqualTo(BLOQUE_INSUMOS_ID);
    }

    @Test
    @DisplayName("Costear una receta vacía no consulta nada y cuesta cero")
    void costearShouldReturnEmptyForEmptyReceta() {
        RecetaCosteada result = recetaService.costear(Receta.builder().lineas(List.of()).build(), NEGOCIO_ID);

        assertThat(result.lineas()).isEmpty();
        assertThat(result.costo()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Costear responde 'no existe' si la columna del precio ya no está")
    void costearShouldFailWhenPriceColumnIsGone() {
        when(campoRepository.findByIdAndNegocioId(PRECIO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
        Receta receta = givenReceta(HARINA_ID);

        assertThatThrownBy(() -> recetaService.costear(receta, NEGOCIO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void givenColumnaDelPrecio(TipoCampo tipo) {
        when(campoRepository.findByIdAndNegocioId(PRECIO_ID, NEGOCIO_ID)).thenReturn(Optional.of(
                Campo.builder().id(PRECIO_ID).bloqueId(BLOQUE_INSUMOS_ID).tipo(tipo).build()));
    }

    private static Registro givenInsumo(RegistroId id) {
        return Registro.builder().id(id).valores(Map.of()).build();
    }

    private static Receta givenReceta(RegistroId... insumos) {
        return Receta.builder()
                .negocioId(NEGOCIO_ID)
                .productoId(PRODUCTO_ID)
                .campoPrecioId(PRECIO_ID)
                .lineas(java.util.Arrays.stream(insumos)
                        .map(insumo -> new LineaReceta(insumo, new BigDecimal("2"), null))
                        .toList())
                .build();
    }
}
