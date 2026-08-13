package com.betacom.jpa.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.betacom.jpa.models.PasswordResetToken;
import com.betacom.jpa.models.Utente;

import jakarta.persistence.LockModeType;

public interface IPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)   // Serve a evitare che due richieste simultanee riescano a usare lo stesso token.
	
	// cerca la richiesta corrispondente al token ricevuto.
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);
	// elimina i vecchi token ancora inutilizzati prima di generarne uno nuovo.
	void deleteByUtenteAndDataUtilizzoIsNull(Utente utente);
}
