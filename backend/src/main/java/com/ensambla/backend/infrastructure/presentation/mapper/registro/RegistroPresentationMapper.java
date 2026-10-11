package com.ensambla.backend.infrastructure.presentation.mapper.registro;

import com.ensambla.backend.domain.dto.registro.CrearRegistroRequest;
import com.ensambla.backend.domain.dto.registro.CrearRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EditarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.EditarRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EliminarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosResponse;
import com.ensambla.backend.domain.dto.registro.ObtenerFotoOrigenRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.infrastructure.presentation.dto.registro.PaginaRegistrosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroRequest;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RegistroPresentationMapper {

    public CrearRegistroRequest toCrearRequest(UUID usuarioId, UUID bloqueId, RegistroRequest request) {
        return new CrearRegistroRequest(usuarioId, bloqueId, request.valores());
    }

    public ListarRegistrosRequest toListarRequest(UUID usuarioId, UUID bloqueId, String texto, String orden,
                                                  String direccion, Integer pagina, Integer tamanio) {
        return new ListarRegistrosRequest(usuarioId, bloqueId, texto, orden, direccion, pagina, tamanio);
    }

    public ObtenerRegistroRequest toObtenerRequest(UUID usuarioId, UUID registroId) {
        return new ObtenerRegistroRequest(usuarioId, registroId);
    }

    public EditarRegistroRequest toEditarRequest(UUID usuarioId, UUID registroId, RegistroRequest request) {
        return new EditarRegistroRequest(usuarioId, registroId, request.valores());
    }

    public ObtenerFotoOrigenRequest toFotoOrigenRequest(UUID usuarioId, UUID registroId) {
        return new ObtenerFotoOrigenRequest(usuarioId, registroId);
    }

    public EliminarRegistroRequest toEliminarRequest(UUID usuarioId, UUID registroId) {
        return new EliminarRegistroRequest(usuarioId, registroId);
    }

    public RegistroResponse toResponse(CrearRegistroResponse response) {
        return toResponse(response.registro());
    }

    public RegistroResponse toResponse(ObtenerRegistroResponse response) {
        return toResponse(response.registro());
    }

    public RegistroResponse toResponse(EditarRegistroResponse response) {
        return toResponse(response.registro());
    }

    public PaginaRegistrosResponse toResponse(ListarRegistrosResponse response) {
        return new PaginaRegistrosResponse(
                response.contenido().stream().map(this::toResponse).toList(),
                response.pagina(),
                response.tamanio(),
                response.totalElementos(),
                response.totalPaginas(),
                response.pagina() == 0,
                response.pagina() >= response.totalPaginas() - 1);
    }

    private RegistroResponse toResponse(RegistroItem item) {
        return new RegistroResponse(
                item.id(), item.bloqueId(), item.valores(), item.fotoOrigenId(), item.creadoEn(),
                item.actualizadoEn());
    }
}
