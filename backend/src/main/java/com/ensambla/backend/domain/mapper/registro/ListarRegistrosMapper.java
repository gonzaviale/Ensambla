package com.ensambla.backend.domain.mapper.registro;

import com.ensambla.backend.domain.dto.registro.ListarRegistrosResponse;
import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.Registro;

public class ListarRegistrosMapper {

    public ListarRegistrosResponse toResponse(Pagina<Registro> pagina) {
        return new ListarRegistrosResponse(
                pagina.contenido().stream().map(RegistroItem::de).toList(),
                pagina.numero(),
                pagina.tamanio(),
                pagina.totalElementos(),
                pagina.totalPaginas());
    }
}
