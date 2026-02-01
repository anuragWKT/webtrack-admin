package com.webknot.webtrak_admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.webknot.webtrak_admin.client")
@EnableScheduling
public class WebtrakAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebtrakAdminApplication.class, args);
	}

}
