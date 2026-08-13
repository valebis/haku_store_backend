package com.betacom.jpa.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// request dopo che l’utente ha aperto il link ricevuto: 

@Getter
@Setter
public class ResetPasswordReq {
	@NotBlank(message = "auth.reset.token.required")
	private String token;

	@NotBlank(message = "auth.no.password")
	@Size(min = 8, message = "utente.password.short")
	private String password;
}
