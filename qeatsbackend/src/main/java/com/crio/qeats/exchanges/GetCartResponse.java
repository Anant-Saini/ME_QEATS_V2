package com.crio.qeats.exchanges;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import com.crio.qeats.models.ItemEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetCartResponse {

    private String userId;
    private List<ItemEntity> items;
    private Integer cartTotal;

    
}
