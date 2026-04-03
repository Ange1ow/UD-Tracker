package com.udtracker.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "System Status", description = "Перевірка працездатності системи")
public class StatusController {

    @GetMapping("/status")
    @Operation(summary = "Отримати статус системи", description = "Повертає повідомлення, що UDTracker працює")
    public String getStatus() {
        return "UDTracker is Online";
    }
}