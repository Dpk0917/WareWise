package com.app.warewise.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class usersResponse {

    private UUID id;
    private String name;
    private String role;
    private String email;
    private String contact;
    private LocalDateTime createdAt;
}
