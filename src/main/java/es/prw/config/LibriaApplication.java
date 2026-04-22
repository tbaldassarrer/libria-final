package es.prw.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages="es.prw")
@EnableJpaRepositories(basePackages = "es.prw.repositories")
@EntityScan(basePackages = "es.prw.models")
public class LibriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibriaApplication.class, args);
	}

}
