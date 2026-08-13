package com.betacom.jpa.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.betacom.jpa.dto.input.LoginReq;
import com.betacom.jpa.dto.input.ForgotPasswordReq;
import com.betacom.jpa.dto.input.ResetPasswordReq;
import com.betacom.jpa.dto.input.UtenteReq;
import com.betacom.jpa.dto.output.AuthResponseDTO;
import com.betacom.jpa.dto.output.ResponseDTO;

import tools.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;

import lombok.extern.slf4j.Slf4j;

// Proprietario: Pier e Valerio
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JavaMailSender mailSender;

	@Test
	@Order(1)
	public void registerTest() throws Exception {
		log.debug("registerTest");

		UtenteReq req = new UtenteReq();
		req.setNome("Mario");
		req.setCognome("Rossi");
		req.setEmail("mario.auth@test.it");
		req.setPassword("password123");

		MvcResult result = mockMvc.perform(post("/rest/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists())
				.andReturn();

		String json = result.getResponse().getContentAsString();
		AuthResponseDTO dto = objectMapper.readValue(json, AuthResponseDTO.class);
		log.debug("rc register: {}", dto.getUtente());
	}

	@Test
	@Order(2)
	public void registerTestError() throws Exception {
		log.debug("registerTestError - email gia' registrata");

		UtenteReq req = new UtenteReq();
		req.setNome("Mario");
		req.setCognome("Rossi");
		req.setEmail("mario.auth@test.it");
		req.setPassword("password123");

		MvcResult result = mockMvc.perform(post("/rest/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.msg").exists())
				.andReturn();

		String json = result.getResponse().getContentAsString();
		ResponseDTO dto = objectMapper.readValue(json, ResponseDTO.class);
		log.debug("rc registerError: {}", dto.getMsg());
	}

	@Test
	@Order(3)
	public void loginTest() throws Exception {
		log.debug("loginTest");

		LoginReq req = new LoginReq();
		req.setEmail("mario.auth@test.it");
		req.setPassword("password123");

		MvcResult result = mockMvc.perform(post("/rest/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists())
				.andReturn();

		String json = result.getResponse().getContentAsString();
		AuthResponseDTO dto = objectMapper.readValue(json, AuthResponseDTO.class);
		log.debug("rc login: {}", dto.getToken());
	}

	@Test
	@Order(4)
	public void loginTestError() throws Exception {
		log.debug("loginTestError - password errata");

		LoginReq req = new LoginReq();
		req.setEmail("mario.auth@test.it");
		req.setPassword("passwordSbagliata");

		MvcResult result = mockMvc.perform(post("/rest/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.msg").exists())
				.andReturn();

		String json = result.getResponse().getContentAsString();
		ResponseDTO dto = objectMapper.readValue(json, ResponseDTO.class);
		log.debug("rc loginError: {}", dto.getMsg());
	}

	@Test
	@Order(5)
	public void passwordResetTest() throws Exception {
		ForgotPasswordReq forgot = new ForgotPasswordReq();
		forgot.setEmail("mario.auth@test.it");

		mockMvc.perform(post("/rest/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(forgot)))
				.andExpect(status().isOk());

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mail.capture());
		String body = mail.getValue().getText();
		String token = body.substring(body.indexOf("?token=") + 7, body.indexOf("\n\n", body.indexOf("?token=")));

		ResetPasswordReq reset = new ResetPasswordReq();
		reset.setToken(token);
		reset.setPassword("nuovaPassword123");
		mockMvc.perform(post("/rest/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reset)))
				.andExpect(status().isOk());

		LoginReq login = new LoginReq();
		login.setEmail("mario.auth@test.it");
		login.setPassword("nuovaPassword123");
		mockMvc.perform(post("/rest/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(login)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/rest/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(reset)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(6)
	public void forgotPasswordDoesNotRevealUnknownEmail() throws Exception {
		clearInvocations(mailSender);
		ForgotPasswordReq forgot = new ForgotPasswordReq();
		forgot.setEmail("inesistente@test.it");

		mockMvc.perform(post("/rest/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(forgot)))
				.andExpect(status().isOk());
		verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
	}

}
