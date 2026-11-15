package com.ensambla.backend.infrastructure.presentation.mapper.negocio;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.ListaNegociosResponse;
import com.ensambla.backend.domain.useCase.negocio.NegocioDeLaCuentaItem;
import com.ensambla.backend.domain.useCase.negocio.UbicacionNegocioItem;
import com.ensambla.backend.domain.useCase.negocio.cambiarNegocioActivo.CambiarNegocioActivoRequest;
import com.ensambla.backend.domain.useCase.negocio.crearNegocio.CrearNegocioRequest;
import com.ensambla.backend.domain.useCase.negocio.eliminarNegocio.EliminarNegocioRequest;
import com.ensambla.backend.domain.useCase.negocio.listarNegocios.ListarNegociosRequest;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.CrearNegocioWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.ListaNegociosWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.NegocioDeLaCuentaWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.UbicacionNegocioWeb;
import org.springframework.stereotype.Component;

@Component
public class NegociosPresentationMapper {

    public ListarNegociosRequest toListarRequest(UsuarioId usuarioId) {
        return new ListarNegociosRequest(usuarioId);
    }

    public CrearNegocioRequest toCrearRequest(UsuarioId usuarioId, CrearNegocioWebRequest request) {
        UbicacionNegocioWeb ubicacion = request.ubicacion();
        UbicacionNegocioItem item = ubicacion == null ? null : new UbicacionNegocioItem(
                ubicacion.direccion(), ubicacion.localidad(), ubicacion.latitud(), ubicacion.longitud());
        return new CrearNegocioRequest(usuarioId, request.nombre(), request.rubro(), item);
    }

    public CambiarNegocioActivoRequest toCambiarRequest(UsuarioId usuarioId, NegocioId negocioId) {
        return new CambiarNegocioActivoRequest(usuarioId, negocioId);
    }

    public EliminarNegocioRequest toEliminarRequest(UsuarioId usuarioId, NegocioId negocioId) {
        return new EliminarNegocioRequest(usuarioId, negocioId);
    }

    public ListaNegociosWebResponse toResponse(ListaNegociosResponse response) {
        return new ListaNegociosWebResponse(response.activoId().toString(), response.limite(), response.puedeCrear(),
                response.negocios().stream().map(this::toResponse).toList());
    }

    public NegocioDeLaCuentaWebResponse toResponse(NegocioDeLaCuentaItem item) {
        return new NegocioDeLaCuentaWebResponse(item.id().toString(), item.nombre(), item.rubro(),
                item.activo(), item.creadoEn());
    }
}
