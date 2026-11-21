package com.ensambla.backend.infrastructure.presentation.mapper.bloque;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import com.ensambla.backend.domain.useCase.bloque.crearBloque.CrearBloqueRequest;
import com.ensambla.backend.domain.useCase.bloque.eliminarBloque.EliminarBloqueRequest;
import com.ensambla.backend.domain.useCase.bloque.listarBloques.ListarBloquesRequest;
import com.ensambla.backend.domain.useCase.bloque.renombrarBloque.RenombrarBloqueRequest;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueResponse;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueWebRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BloquePresentationMapperTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private final BloquePresentationMapper mapper = new BloquePresentationMapper();

    @Test
    @DisplayName("Arma el pedido de listar con el usuario y el proyecto")
    void armaElPedidoDeListar() {
        entoncesElPedidoEs(mapper.toListarRequest(USUARIO_ID, PROYECTO_ID), new ListarBloquesRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("Arma el pedido de crear con el nombre del body")
    void armaElPedidoDeCrearConElNombre() {
        entoncesElPedidoEs(mapper.toCrearRequest(USUARIO_ID, PROYECTO_ID, new BloqueWebRequest("Clientes")),
                new CrearBloqueRequest(USUARIO_ID, PROYECTO_ID, "Clientes"));
    }

    @Test
    @DisplayName("Arma el pedido de renombrar con el bloque y el nombre del body")
    void armaElPedidoDeRenombrarConElBloqueYElNombre() {
        entoncesElPedidoEs(mapper.toRenombrarRequest(USUARIO_ID, BLOQUE_ID, new BloqueWebRequest("Compradores")),
                new RenombrarBloqueRequest(USUARIO_ID, BLOQUE_ID, "Compradores"));
    }

    @Test
    @DisplayName("Arma el pedido de eliminar con el usuario y el bloque")
    void armaElPedidoDeEliminar() {
        entoncesElPedidoEs(mapper.toEliminarRequest(USUARIO_ID, BLOQUE_ID), new EliminarBloqueRequest(USUARIO_ID, BLOQUE_ID));
    }

    @Test
    @DisplayName("Pasa todos los datos del bloque a la respuesta HTTP")
    void pasaTodosLosDatosDelBloqueALaRespuesta() {
        entoncesElBloqueSeRespondeComo(unBloque("Productos"),
                new BloqueResponse(BLOQUE_ID, PROYECTO_ID, "Productos", (short) 1, CREADO, ACTUALIZADO));
    }

    @Test
    @DisplayName("Una lista conserva el orden")
    void unaListaConservaElOrden() {
        entoncesLaListaTieneLosNombres(List.of(unBloque("Primero"), unBloque("Segundo")), "Primero", "Segundo");
    }

    @Test
    @DisplayName("Una lista vacía queda vacía")
    void unaListaVaciaQuedaVacia() {
        entoncesLaListaTieneLosNombres(List.of());
    }

    private static BloqueItem unBloque(String nombre) {
        return new BloqueItem(BLOQUE_ID, PROYECTO_ID, nombre, (short) 1, CREADO, ACTUALIZADO);
    }

    private static void entoncesElPedidoEs(Object armado, Object esperado) {
        assertThat(armado).isEqualTo(esperado);
    }

    private void entoncesElBloqueSeRespondeComo(BloqueItem bloque, BloqueResponse esperada) {
        assertThat(mapper.toResponse(bloque)).isEqualTo(esperada);
    }

    private void entoncesLaListaTieneLosNombres(List<BloqueItem> bloques, String... nombres) {
        assertThat(mapper.toResponse(bloques)).extracting(BloqueResponse::nombre).containsExactly(nombres);
    }
}