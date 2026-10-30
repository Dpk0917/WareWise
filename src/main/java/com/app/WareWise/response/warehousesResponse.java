package com.app.warewise.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class warehousesResponse {

    private Long id;
    private String name;
    private String city;
    private String pincode;

}
