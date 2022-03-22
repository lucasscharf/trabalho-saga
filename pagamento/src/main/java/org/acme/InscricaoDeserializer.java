package org.acme;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class InscricaoDeserializer extends ObjectMapperDeserializer<Inscricao> {

	public InscricaoDeserializer() {
		super(Inscricao.class);
	}

}
