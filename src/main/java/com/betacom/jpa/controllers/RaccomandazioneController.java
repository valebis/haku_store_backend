package com.betacom.jpa.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.betacom.jpa.security.UtentePrincipal;
import com.betacom.jpa.services.interfaces.IRaccomandazioneServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Proprietario: Valerio
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/rest/raccomandazione")
public class RaccomandazioneController {

	private final IRaccomandazioneServices raccS;

	@GetMapping("/list")
	public ResponseEntity<Object> list(
			@RequestParam(
					required = false,
					defaultValue = "10"
			) Integer limite,
			@AuthenticationPrincipal UtentePrincipal principal
	) throws Exception {

		return ResponseEntity.ok(
				raccS.getRaccomandazioni(
						principal.getIdUtente(),
						limite
				)
		);
	}
}