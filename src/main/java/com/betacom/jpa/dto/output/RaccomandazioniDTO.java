package com.betacom.jpa.dto.output;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Proprietario: Valerio
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RaccomandazioniDTO {

	private Integer idUtente;

	private List<Raccomandazione> raccomandazioni;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	public static class Raccomandazione {

		private Integer idVariante;
		private Integer idProdotto;
		private String nomeProdotto;
		private String categoria;
		private String marca;
		private String gusto;
		private String formato;
		private BigDecimal prezzo;
		private Double score;
	}
}