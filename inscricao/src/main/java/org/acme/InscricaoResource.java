package org.acme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

@Path("/inscricao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InscricaoResource {
    static Set<Inscricao> inscricoes = new HashSet<>();

    @Inject
    @Channel("inscricao-realizada")
    Emitter<Inscricao> emitter;

    private static final Logger logger = LoggerFactory.getLogger(InscricaoResource.class);

    @Incoming("inscricao-atualizada")
    public void atualizarInscricao(Inscricao inscricao) {
        inscricoes.add(inscricao);
        logger.info("Atualizando inscrição: [{}]", inscricao);
    }

    @POST
    public Response cadastrarInscricao(Inscricao inscricao) {
        inscricao.setStatus("Pendente");
        inscricao.setDescricao("");
        inscricoes.add(inscricao);
        emitter.send(inscricao);

        logger.info("Cadastrando inscrição: [{}]", inscricao);
        return Response.ok().build();
    }

    @GET
    public Response pegarTodos() {
        logger.info("Recuperando todas as inscrições. Tamanho da lista [{}]", inscricoes.size());
        return Response.ok(inscricoes).build();
    }

    @Path("{id}")
    @GET
    public Response pegar(@PathParam("id") Integer id) {
        logger.info("Recuperando inscrição com ID: [{}]", id);
        Optional<Inscricao> inscricao = inscricoes.stream().filter(i -> i.getId().equals(id)).findAny();
        logger.info("Inscrição recuperada [{}]", inscricao);
        if (inscricao.isPresent()) {
            return Response.ok(inscricao.get()).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }
}