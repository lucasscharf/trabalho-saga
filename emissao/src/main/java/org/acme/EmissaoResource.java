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

@Path("/emissao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmissaoResource {
    static Set<Pagamento> pagamentos = new HashSet<>();

    @Inject
    @Channel("emissao-realizada")
    Emitter<Pagamento> emitterEmissao;

    @Inject
    @Channel("inscricao-atualizada")
    Emitter<Inscricao> emitterInscricaoAtualizada;

    @Inject
    @Channel("pagamento-atualizado")
    Emitter<Pagamento> emitterPagamentoAtualizado;

    private static final Logger logger = LoggerFactory.getLogger(EmissaoResource.class);

    @Incoming("pagamento-realizado")
    public void atualizarInscricao(Pagamento pagamento) {
        pagamentos.add(pagamento);
        logger.info("Atualizando pagamento: [{}]", pagamento);
    }

    @POST
    @Path("/emitirComSucesso/{id}")
    public Response pagarComSucesso(@PathParam("id") Integer id) {
        logger.info("Recuperando pagamento com ID: [{}]", id);
        Optional<Pagamento> pagamentoOptional = pagamentos.stream().filter(i -> i.getId().equals(id)).findAny();
        logger.info("Pagamento recuperado [{}]", pagamentoOptional);
        if (pagamentoOptional.isPresent()) {
            Pagamento pagamento = pagamentoOptional.get();
            pagamento.setDescricao("");
            pagamento.setStatus("EMISSAO_REALIZADA");
            pagamentos.add(pagamento);

            emitterEmissao.send(pagamento);
            emitterPagamentoAtualizado.send(pagamento);

            Inscricao inscricao = pagamento.getInscricao();
            inscricao.setStatus("EMISSAO_REALIZADA");
            inscricao.setDescricao("");

            emitterInscricaoAtualizada.send(inscricao);
            return Response.ok(pagamento).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    @POST
    @Path("/emitorComFalha/{id}")
    public Response pagarComFalha(@PathParam("id") Integer id, String descricao) {
        logger.info("Recuperando pagamento com ID: [{}]", id);
        Optional<Pagamento> pagamentoOptional = pagamentos.stream().filter(i -> i.getId().equals(id)).findAny();
        logger.info("Pagamento recuperado [{}]", pagamentoOptional);
        if (pagamentoOptional.isPresent()) {
            Pagamento pagamento = new Pagamento();
            pagamento.setStatus("PAGAMENTO_CANCELADO");
            pagamento.setDescricao(descricao);

            emitterPagamentoAtualizado.send(pagamento);

            pagamentos.add(pagamento);

            Inscricao inscricao = pagamento.getInscricao();
            inscricao.setDescricao("Problemas na emissão de nota");
            inscricao.setStatus("INSCRICAO_CANCELADA");
            emitterInscricaoAtualizada.send(inscricao);

            return Response.ok(inscricao).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    @GET
    public Response pegarTodos() {
        logger.info("Recuperando todos os pagamentos. Tamanho da lista [{}]", pagamentos.size());
        return Response.ok(pagamentos).build();
    }

    @Path("/inscricao")
    @GET
    public Response pegarInscricao() {
        logger.info("Recuperando todas as inscrições. Tamanho da lista [{}]", pagamentos.size());
        return Response.ok(pagamentos).build();
    }

    @Path("{id}")
    @GET
    public Response pegar(@PathParam("id") Integer id) {
        logger.info("Recuperando pagamento com ID: [{}]", id);
        Optional<Pagamento> pagamento = pagamentos.stream().filter(i -> i.getId().equals(id)).findAny();
        logger.info("Pagamento recuperado [{}]", pagamento);
        if (pagamento.isPresent()) {
            return Response.ok(pagamento.get()).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }
}