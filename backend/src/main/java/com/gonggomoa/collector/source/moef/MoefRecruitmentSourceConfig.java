package com.gonggomoa.collector.source.moef;

import java.time.Duration;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.gonggomoa.collector.source.RecruitmentSource;

@Configuration
public class MoefRecruitmentSourceConfig {

	@Bean
	public RecruitmentSource moefRecruitmentSource(RestClient.Builder restClientBuilder, MoefProperties properties) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
				.withConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
				.withReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
		RestClient restClient = restClientBuilder
				.requestFactory(ClientHttpRequestFactories.get(settings))
				.build();
		return new MoefRecruitmentSource(restClient, properties);
	}
}
