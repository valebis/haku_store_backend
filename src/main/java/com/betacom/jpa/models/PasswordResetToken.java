package com.betacom.jpa.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_token")
	private Long idToken;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_utente", nullable = false)
	private Utente utente;

	@Column(name = "token_hash", length = 64, nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "data_scadenza", nullable = false)
	private LocalDateTime dataScadenza;

	@Column(name = "data_utilizzo")
	private LocalDateTime dataUtilizzo;
}
