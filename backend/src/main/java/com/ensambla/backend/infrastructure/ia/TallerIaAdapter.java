package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.port.TallerIaPort;

/**
 * IA del taller de Sami: arma el pedido con {@link PromptTaller}, hace una única llamada al modelo con herramientas y
 * devuelve lo que contestó junto con lo que costó. Lo que el modelo pidió se valida en el dominio, no acá.
 */
public class TallerIaAdapter implements TallerIaPort {

    private final ClienteModeloConHerramientas cliente;
    private final PromptTaller prompt;

    public TallerIaAdapter(ClienteModeloConHerramientas cliente, PromptTaller prompt) {
        this.cliente = cliente;
        this.prompt = prompt;
    }

    @Override
    public ResultadoTallerIa conversar(ConsultaTaller consulta) {
        RespuestaConHerramientas respuesta = cliente.completar(prompt.peticion(consulta));
        ConsumoIaSami consumo = new ConsumoIaSami(
                respuesta.proveedor(), respuesta.modelo(), respuesta.tokensEntrada(), respuesta.tokensSalida());
        return new ResultadoTallerIa(respuesta.texto(), respuesta.llamadas(), consumo);
    }
}
