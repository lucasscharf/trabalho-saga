package org.acme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LogConsumer {
    private static final Logger logger = LoggerFactory.getLogger(LogConsumer.class);

    @Incoming("pagamento-realizado")
    public void pagamentoRealizado(Pagamento pagamento) {
        logger.info("Pagamento realizado: [{}]", pagamento);
    }

    @Incoming("emissao-realizada")
    public void emissaoRealizada(Pagamento pagamento) {
        logger.info("Emissão realizada: [{}]", pagamento);
    }

    @Incoming("pagamento-atualizado")
    public void pagamentoAtualizado(Pagamento pagamento) {
        logger.info("Pagamento atualizado: [{}]", pagamento);
    }

    //
    @Incoming("inscricao-realizada")
    public void inscricaoRealizada(Inscricao inscricao) {
        logger.info("Inscrição realizada: [{}]", inscricao);
    }

    @Incoming("inscricao-atualizada")
    public void inscricaoAtualizada(Inscricao inscricao) {
        logger.info("Inscrição atualizada: [{}]", inscricao);
    }
}