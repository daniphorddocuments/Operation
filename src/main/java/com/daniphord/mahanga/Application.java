// hii ni package yako kuu
package com.daniphord.mahanga;

// Imports muhimu za Spring Boot
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Anza Spring Boot application
@SpringBootApplication
@EnableScheduling
public class Application {

	public static void main(String[] args) {
		// Inaanza application yako
		SpringApplication.run(Application.class, args);
	}
}
