package org.acme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

@Path("/acesso")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AcessoResource {
    static Set<Inscricao> inscricoes = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(AcessoResource.class);

    @Incoming("emissao-realizada")
    public void atualizarInscricao(Pagamento pagamento) {
        inscricoes.add(pagamento.getInscricao());
        logger.info("Atualizando inscrição: [{}]", pagamento);
    }

    @GET
    @Path("/{id}")
    public Response pagarComSucesso(@PathParam("id") Integer id) {
        Inscricao inscricao = new Inscricao();
        inscricao.setId(id);
        return Response.ok(inscricoes.contains(inscricao)).build();
    }
}