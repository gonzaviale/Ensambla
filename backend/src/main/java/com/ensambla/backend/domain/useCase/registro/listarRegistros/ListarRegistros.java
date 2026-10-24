package com.ensambla.backend.domain.useCase.registro.listarRegistros;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarRegistros {

    private final NegocioService negocioService;
    private final BloqueService bloqueService;
    private final CampoService campoService;
    private final RegistroDatosService registroDatosService;

    public ListarRegistrosResponse execute(ListarRegistrosRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        ConsultaRegistros consulta = ConsultaRegistros.armar(
                negocioId,
                bloque.getId(),
                campos,
                request.texto(),
                request.orden(),
                request.direccion(),
                request.pagina(),
                request.tamanio());
        Pagina<Registro> pagina = registroDatosService.listar(consulta);
        TotalesRegistros totales = registroDatosService.totales(consulta, campos, pagina.totalElementos());
        return ListarRegistrosResponse.de(pagina.map(registro -> registro.conFormulas(campos)), totales);
    }
}
