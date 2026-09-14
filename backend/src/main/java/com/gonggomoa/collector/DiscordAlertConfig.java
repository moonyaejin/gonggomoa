package com.gonggomoa.collector;

import java.time.Duration;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class DiscordAlertConfig {

	@Bean
	public RestClient discordAlertRestClient(RestClient.Builder restClientBuilder, DiscordAlertProperties properties) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
				.withConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
				.withReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
		return restClientBuilder
				.requestFactory(ClientHttpRequestFactories.get(settings))
				.build();
	}
}
