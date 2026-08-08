package com.betacom.jpa.services.interfaces;

import com.betacom.jpa.dto.output.RaccomandazioniDTO;

// Proprietario: Valerio
public interface IRaccomandazioneServices {

	RaccomandazioniDTO getRaccomandazioni(
			Integer idUtente,
			Integer limite
	) throws Exception;
}