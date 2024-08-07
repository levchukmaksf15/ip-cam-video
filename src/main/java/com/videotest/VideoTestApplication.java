package com.videotest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class VideoTestApplication {

	public static void main(String[] args) {
//		SpringApplication.run(VideoTestApplication.class, args);
		SpringApplicationBuilder builder = new SpringApplicationBuilder(VideoTestApplication.class);

		builder.headless(false);

		ConfigurableApplicationContext context = builder.run(args);
	}

}
