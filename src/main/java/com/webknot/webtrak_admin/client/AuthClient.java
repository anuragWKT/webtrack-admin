package com.webknot.webtrak_admin.client;

import com.webknot.webtrak_admin.dto.AuthUserResponse;
import com.webknot.webtrak_admin.config.AuthFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth.service.url}", configuration = AuthFeignConfig.class)
public interface AuthClient {

    @GetMapping("/users/{id}")
    AuthUserResponse getUserById(@PathVariable("id") Long id);

    @GetMapping("/users/email/{email}")
    AuthUserResponse getUserByEmail(@PathVariable("email") String email);
}
