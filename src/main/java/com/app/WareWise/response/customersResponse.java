package com.app.warewise.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class customersResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;

}
