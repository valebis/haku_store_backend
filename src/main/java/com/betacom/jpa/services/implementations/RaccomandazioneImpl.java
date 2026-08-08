package com.betacom.jpa.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.betacom.jpa.dto.output.RaccomandazioniDTO;
import com.betacom.jpa.services.interfaces.IRaccomandazioneServices;

import lombok.extern.slf4j.Slf4j;

// Proprietario: Valerio
@Slf4j
@Service
public class RaccomandazioneImpl implements IRaccomandazioneServices {

	private final RestClient restClient;

	public RaccomandazioneImpl() {

		this.restClient = RestClient.builder()
				.baseUrl("http://localhost:5000")
				.build();
	}

	@Override
	public RaccomandazioniDTO getRaccomandazioni(
			Integer idUtente,
			Integer limite
	) throws Exception {

		log.debug(
				"richiesta raccomandazioni utente: {}",
				idUtente
		);

		RaccomandazioniDTO response = restClient
				.get()
				.uri(uriBuilder -> uriBuilder
						.path("/raccomandazioni/{idUtente}")
						.queryParam("limite", limite)
						.build(idUtente)
				)
				.retrieve()
				.body(RaccomandazioniDTO.class);

		return response;
	}
}