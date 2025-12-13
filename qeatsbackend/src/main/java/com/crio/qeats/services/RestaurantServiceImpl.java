
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  private static final LocalTime MORNING_START = LocalTime.of(8, 0);
  private static final LocalTime MORNING_END = LocalTime.of(10, 0);

  private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
  private static final LocalTime AFTERNOON_END = LocalTime.of(14, 0);

  private static final LocalTime EVENING_START = LocalTime.of(19, 0);
  private static final LocalTime EVENING_END = LocalTime.of(21, 0);

  @Autowired
  @Qualifier("restaurantRepositoryServiceImpl")
  private RestaurantRepositoryService restaurantRepositoryService;


  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

        Double servingRadiusInKms;

        if(isPeakHours(currentTime)) {
          servingRadiusInKms = peakHoursServingRadiusInKms;
        } else {
          servingRadiusInKms = normalHoursServingRadiusInKms;
        }

        List<Restaurant> restaurants = restaurantRepositoryService.findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude()
        , currentTime, servingRadiusInKms);

     return new GetRestaurantsResponse(restaurants);
  }

  private boolean isPeakHours(LocalTime currentTime) {

    return isWithin(currentTime, MORNING_START, MORNING_END) || 
    isWithin(currentTime, AFTERNOON_START, AFTERNOON_END) || 
    isWithin(currentTime, EVENING_START, EVENING_END);
  }

  private boolean isWithin(LocalTime time, LocalTime start, LocalTime end) {
    return !time.isBefore(start) && (time.isBefore(end) || time.equals(end));
  }


}

