package com.betacom.jpa.dto.input;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// quando l’utente ha dimenticato la password e inserisce la propria email:
@Getter
@Setter
public class ForgotPasswordReq {
	@NotBlank(message = "auth.no.email")
	@Email(message = "utente.email.invalid")
	private String email;
}
