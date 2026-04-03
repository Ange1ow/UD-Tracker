package com.udtracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class UserProfileDto {
    private String email;
    private Double globalRating;
    private List<LinkedAccountDto> accounts;
}