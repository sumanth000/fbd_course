package com.fdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.fdb.resource"})
public class FdbApplication {

	public static void main(String[] args) {
		SpringApplication.run(FdbApplication.class, args);
	}

}
