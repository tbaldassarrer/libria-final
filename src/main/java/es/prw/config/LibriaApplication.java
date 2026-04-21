package es.prw.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages="es.prw")

public class LibriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibriaApplication.class, args);
	}

}
