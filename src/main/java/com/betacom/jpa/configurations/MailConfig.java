package com.betacom.jpa.configurations;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

// Crea e configura l’oggetto JavaMailSender, cioè il componente utilizzato da Spring per collegarsi al server SMTP
@Configuration
public class MailConfig {
	@Bean
	JavaMailSender javaMailSender(
			@Value("${spring.mail.host:localhost}") String host,
			@Value("${spring.mail.port:1025}") int port,
			@Value("${spring.mail.username:}") String username,
			@Value("${spring.mail.password:}") String password,
			@Value("${spring.mail.properties.mail.smtp.auth:false}") boolean auth,
			@Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTls) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(host);
		sender.setPort(port);
		sender.setUsername(username);
		sender.setPassword(password);
		Properties properties = sender.getJavaMailProperties();
		properties.put("mail.smtp.auth", Boolean.toString(auth));
		properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
		return sender;
	}
}
