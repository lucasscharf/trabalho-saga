package org.acme;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PagamentoDeserializer extends ObjectMapperDeserializer<Pagamento> {

	public PagamentoDeserializer() {
		super(Pagamento.class);
	}

}
