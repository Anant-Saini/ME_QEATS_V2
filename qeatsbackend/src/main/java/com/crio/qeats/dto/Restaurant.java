
package com.crio.qeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
public class Restaurant {

    @NotNull
    private String restaurantId;
    private String name;
    private String city;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private String opensAt;
    private String closesAt;
    private List<String> attributes;

}

