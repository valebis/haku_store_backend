package com.betacom.jpa.services.implementations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.jpa.dto.input.ForgotPasswordReq;
import com.betacom.jpa.dto.input.LoginReq;
import com.betacom.jpa.dto.input.ResetPasswordReq;
import com.betacom.jpa.dto.input.UtenteReq;
import com.betacom.jpa.dto.output.AuthResponseDTO;
import com.betacom.jpa.exceptions.ApiException;
import com.betacom.jpa.mapping.UtenteMap;
import com.betacom.jpa.models.Utente;
import com.betacom.jpa.models.PasswordResetToken;
import com.betacom.jpa.repositories.IPasswordResetTokenRepository;
import com.betacom.jpa.repositories.IUtenteRepository;
import com.betacom.jpa.security.JwtService;
import com.betacom.jpa.security.UtentePrincipal;
import com.betacom.jpa.services.interfaces.IAuthServices;
import com.betacom.jpa.services.interfaces.IUtenteServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Proprietario: Sarah
@Slf4j
@RequiredArgsConstructor
@Service
public class AuthImpl implements IAuthServices {

	private final IUtenteServices utenteS;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final IUtenteRepository utenteR;
	private final IPasswordResetTokenRepository resetTokenR;
	private final JavaMailSender mailSender;
	private final SecureRandom secureRandom = new SecureRandom();

	@Value("${app.mail.from}")
	private String mailFrom;

	@Value("${app.password-reset.url}")
	private String resetUrl;

	@Value("${app.password-reset.expiration-minutes:30}")
	private long resetExpirationMinutes;

	@Override
	public AuthResponseDTO register(UtenteReq req) throws Exception {
		log.debug("register {}", req);
		utenteS.create(req);
		Utente ut = utenteS.getEntityByEmail(req.getEmail());
		return buildAuthResponse(ut);
	}

	@Override
	public AuthResponseDTO login(LoginReq req) throws Exception {
		log.debug("login {}", req.getEmail());
		Utente ut = utenteS.getEntityByEmail(req.getEmail());

		if (!passwordEncoder.matches(req.getPassword(), ut.getPassword()))
			throw new ApiException("auth.badcredentials");

		return buildAuthResponse(ut);
	}

	// riceve l’email, crea un token temporaneo e invia il link.
	@Override
	@Transactional
	public void forgotPassword(ForgotPasswordReq req) {
		// La stessa risposta viene restituita anche per email sconosciute, evitando account enumeration.
		utenteR.findByEmail(req.getEmail().trim()).ifPresent(utente -> {
			resetTokenR.deleteByUtenteAndDataUtilizzoIsNull(utente); // Elimina i precedenti token non ancora utilizzati dello stesso utente.

			// Generazione casuale del token con secureRandom
			byte[] randomBytes = new byte[32];
			secureRandom.nextBytes(randomBytes);
			String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

			// l’oggetto che sarà salvato nella tabella password_reset_token.
			PasswordResetToken resetToken = new PasswordResetToken();
			resetToken.setUtente(utente);
			resetToken.setTokenHash(hashToken(rawToken));
			// per la scadenza Prende l’orario corrente e aggiunge il numero di minuti configurato
			resetToken.setDataScadenza(LocalDateTime.now().plusMinutes(resetExpirationMinutes));
			resetTokenR.save(resetToken);  // dataUtilizzo rimane null perché il token non è stato ancora usato
 
			// SimpleMailMessage rappresenta un’email testuale
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(mailFrom);
			message.setTo(utente.getEmail());
			message.setSubject("Haku Store - Recupero password");
			message.setText("Ciao " + utente.getNome() + ",\n\n"
					+ "usa questo link per scegliere una nuova password:\n"
					+ resetUrl + "?token=" + rawToken + "\n\n"   // url dell'endpoint di reset password (con token non hashato)
					+ "Il link scade tra " + resetExpirationMinutes + " minuti e può essere usato una sola volta.\n"
					+ "Se non hai richiesto tu il recupero, ignora questa email.");
			mailSender.send(message);   // spedisce mail 
		});
	}

	@Override
	@Transactional
	public void resetPassword(ResetPasswordReq req) {
		PasswordResetToken resetToken = resetTokenR.findByTokenHash(hashToken(req.getToken()))
				.orElseThrow(() -> new ApiException("auth.reset.token.invalid"));

		LocalDateTime now = LocalDateTime.now();
		if (resetToken.getDataUtilizzo() != null || !resetToken.getDataScadenza().isAfter(now))
			throw new ApiException("auth.reset.token.invalid");

		resetToken.getUtente().setPassword(passwordEncoder.encode(req.getPassword()));
		resetToken.setDataUtilizzo(now);
		// Invalida anche eventuali altri token ancora attivi per lo stesso account.
		resetTokenR.save(resetToken);
	}

	private String hashToken(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 non disponibile", e);
		}
	}

	private AuthResponseDTO buildAuthResponse(Utente ut) {
		UtentePrincipal principal = new UtentePrincipal(ut);
		String token = jwtService.generateToken(principal);

		return AuthResponseDTO.builder()
				.token(token)
				.tokenType("Bearer")
				.expiresIn(jwtService.getExpirationMs() / 1000)
				.utente(UtenteMap.buildUtenteDTO(ut))
				.build();
	}

}
