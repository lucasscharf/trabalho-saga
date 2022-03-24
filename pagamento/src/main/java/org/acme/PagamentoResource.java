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
        inscricoes.remove(inscricao);
        inscricoes.add(inscricao);
        logger.info("Inscrição cadastrada: [{}]", inscricao);
    }

    @Incoming("pagamento-atualizado")
    public void atualizarPagamento(Pagamento pagamento) {
        logger.info("Atualizando pagamento: [{}]", pagamento);
        pagamentos.stream() //
        .filter(p-> p.getId().equals(pagamento.getId()))
        .findAny()
        .ifPresent(p -> {
            p.setDescricao(pagamento.getDescricao());
            p.setStatus(pagamento.getStatus());
            p.getInscricao().setDescricao(pagamento.getInscricao().getDescricao());
            p.getInscricao().setStatus(pagamento.getInscricao().getStatus());
        });
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
            inscricao.setStatus("PAGAMENTO_REALIZADO");
            inscricoes.add(inscricao);

            emitterAtualizacao.send(inscricao);

            Pagamento pagamento = new Pagamento();
            pagamento.setInscricao(inscricao);
            pagamento.setStatus("PAGAMENTO_REALIZADO");
            pagamento.setId(Pagamento.counter);
            pagamento.setDescricao("");

            pagamentos.add(pagamento);
            emitterPagamento.send(pagamento);
            return Response.ok(pagamento).build();
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
            inscricao.setStatus("CANCELADA");
            inscricoes.add(inscricao);

            emitterAtualizacao.send(inscricao);

            Pagamento pagamento = new Pagamento();
            pagamento.setInscricao(inscricao);
            pagamento.setStatus("PAGAMENTO_CANCELADO");
            pagamento.setDescricao(descricao);

            pagamentos.add(pagamento);

            return Response.ok(pagamento).build();
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
        logger.info("Recuperando todas as inscrições. Tamanho da lista [{}]", inscricoes.size());
        return Response.ok(inscricoes).build();
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