package com.smart.mobility.smartmobilitynotificationservice.client;

import com.smart.mobility.smartmobilitynotificationservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-mobility-pass-service")
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    UserResponse getUserById(@PathVariable("id") String id);
}
