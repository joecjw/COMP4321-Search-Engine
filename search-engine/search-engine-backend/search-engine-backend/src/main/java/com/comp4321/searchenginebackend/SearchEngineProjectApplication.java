package com.comp4321.searchenginebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class SearchEngineProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(SearchEngineProjectApplication.class, args);
	}

}
