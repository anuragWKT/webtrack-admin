package com.webknot.webtrak_admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.webknot.webtrak_admin.client")
public class WebtrakAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebtrakAdminApplication.class, args);
	}

}
