
package com.crio.qeats.exchanges;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class GetRestaurantsRequest {

    
    @NonNull @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;
    
    @NonNull @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;
    private String searchFor;
    
}

