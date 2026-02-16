package com.towork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ToWorkApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToWorkApplication.class, args);
	}
}
