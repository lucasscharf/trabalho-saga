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
import org.slf4j.LoggerFactory;

import main.java.org.acme.Inscricao;
import main.java.org.acme.Pagamento;

@Path("/pagamento")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PagamentoResource {
    static Set<Inscricao> inscricoes = new HashSet<>();
    static List<Pagamento> pagamentos = new ArrayList<>();

    @Inject
    @Channel("pagamento-realizado")
    Emitter<Pagamento> emitterPagamento;

    @Inject
    @Channel("inscricao-atualizada")
    Emitter<Inscricao> emitterAtualizacao;

    private static final Logger logger = LoggerFactory.getLogger(PagamentoResource.class);

    @Incoming("inscricao-realizada")
    public void atualizarInscricao(Inscricao inscricao) {
        inscricoes.add(inscricao);
        logger.info("Atualizando inscrição: [{}]", inscricao);
    }

    @POST
    @Path("/pagarComSucesso/{id}")
    public Response pagarComSucesso(@PathParam("id") Integer id) {
        logger.info("Recuperando inscrição com ID: [{}]", id);
        Optional<Inscricao> inscricaoOptional = inscricoes.stream().filter(i -> i.getId().equals(id)).findAny();
        logger.info("Inscrição recuperada [{}]", inscricaoOptional);
        if (inscricaoOptional.isPresent()) {
            Inscricao inscricao = inscricaoOptional.get();
            inscricao.setDescricao("");
            inscricao.setStatus("Pagamento Realizado");
            inscricoes.add(inscricao);

            emitterAtualizacao.send(inscricao);
            emitterPagamento.send(inscricao);
            return Response.ok(inscricao).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    @POST
    @Path("/pagarComFalha/{id}")
    public Response pagarComFalha(@PathParam("id") Integer id, String descricao) {
        logger.info("Recuperando inscrição com ID: [{}]", id);
        Optional<Inscricao> inscricaoOptional = inscricoes.stream().filter(i -> i.getId().equals(id)).findAny();
        logger.info("Inscrição recuperada [{}]", inscricaoOptional);
        if (inscricaoOptional.isPresent()) {
            Inscricao inscricao = inscricaoOptional.get();
            inscricao.setDescricao(descricao);
            inscricao.setStatus("Cancelada");
            inscricoes.add(inscricao);

            emitterAtualizacao.send(inscricao);
            emitterPagamento.send(inscricao);
            return Response.ok(inscricao).build();
        }
        return Response.status(Status.NOT_FOUND).build();
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