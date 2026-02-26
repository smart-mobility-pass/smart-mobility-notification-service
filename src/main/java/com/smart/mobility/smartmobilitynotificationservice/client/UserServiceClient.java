package com.smart.mobility.smartmobilitynotificationservice.client;

import com.smart.mobility.smartmobilitynotificationservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${feign.client.config.user-service.url:http://localhost:8081/api/v1/users}")
public interface UserServiceClient {

    @GetMapping("/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);
}
