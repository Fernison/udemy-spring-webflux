package com.fernison.springboot.webflux.client.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;

@Configuration
public class AppConfig {

	@Value("${config.base.endpoint}")
	private String url;
	
	@Bean
	@LoadBalanced
	public WebClient.Builder registrarWebClient() {
		return WebClient.builder().baseUrl(url); // Crea el Web client con la ruta por defecto a la que se va a conectar. El WebCLient implementa ya el balanceo al usarl LoadBalanced y Builder
	}
	
}
