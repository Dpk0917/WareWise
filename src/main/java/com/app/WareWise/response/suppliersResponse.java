package com.app.warewise.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class suppliersResponse {

    private Long id;
    private String name;
    private String contact;
}