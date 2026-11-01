package com.ensambla.backend.domain.useCase.registro.listarRegistros;

import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.useCase.registro.RegistroItem;
import com.ensambla.backend.domain.useCase.registro.TotalesItem;

import java.util.List;

public record ListarRegistrosResponse(
        List<RegistroItem> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        TotalesItem totales) {

    public static ListarRegistrosResponse de(Pagina<Registro> pagina, TotalesRegistros totales) {
        return new ListarRegistrosResponse(
                pagina.contenido().stream().map(RegistroItem::de).toList(),
                pagina.numero(),
                pagina.tamanio(),
                pagina.totalElementos(),
                pagina.totalPaginas(),
                TotalesItem.de(totales));
    }
}
