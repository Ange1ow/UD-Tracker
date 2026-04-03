package com.udtracker.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories("com.udtracker.api.repository") // Вказуємо Spring, де шукати твій репозиторій
@EnableScheduling
public class UdTrackerApplication {
	public static void main(String[] args) {
		SpringApplication.run(UdTrackerApplication.class, args);
	}
}