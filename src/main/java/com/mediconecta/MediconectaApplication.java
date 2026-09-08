package com.mediconecta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling habilita los metodos @Scheduled, como el snapshot horario
// de UsuariosActivosService.
@SpringBootApplication
@EnableScheduling
public class MediconectaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediconectaApplication.class, args);
	}

}
