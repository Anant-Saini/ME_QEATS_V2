
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  private static final Integer MAX_THREADS = 4;
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

        Double servingRadiusInKms = getServingRadiusInKms(currentTime);

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

  private Double getServingRadiusInKms(LocalTime currentTime) {
    Double servingRadiusInKms = normalHoursServingRadiusInKms;

    if(isPeakHours(currentTime)) 
      servingRadiusInKms = peakHoursServingRadiusInKms;

    return servingRadiusInKms;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

      // Double latitude = getRestaurantsRequest.getLatitude();
      // Double longitude = getRestaurantsRequest.getLongitude();
      // String searchString = getRestaurantsRequest.getSearchFor();
      // if(searchString == null || searchString.isEmpty())
      //   return new GetRestaurantsResponse(new ArrayList<>());
      // Double servingRadiusInKms = getServingRadiusInKms(currentTime);
      // List<Restaurant> list1 = restaurantRepositoryService.findRestaurantsByName(latitude, longitude, searchString, currentTime, servingRadiusInKms);
      // List<Restaurant> list2 = restaurantRepositoryService.findRestaurantsByAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms);
      // List<Restaurant> list3 = restaurantRepositoryService.findRestaurantsByItemName(latitude, longitude, searchString, currentTime, servingRadiusInKms);
      // List<Restaurant> list4 = restaurantRepositoryService.findRestaurantsByItemAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms);

      // return new GetRestaurantsResponse(getOrderedRestaurantsList(list1, list2, list3, list4));
      return findRestaurantsBySearchQueryMt(getRestaurantsRequest, currentTime); //Just for the Multithreading module
  }

  private List<Restaurant> getOrderedRestaurantsList(List<Restaurant> list1, List<Restaurant> list2, List<Restaurant> list3, List<Restaurant> list4) {

    return Stream.of(list1, list2, list3, list4).flatMap(List::stream).distinct().collect(Collectors.toList());

  }

  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

      Double latitude = getRestaurantsRequest.getLatitude();
      Double longitude = getRestaurantsRequest.getLongitude();
      String searchString = getRestaurantsRequest.getSearchFor();
      if(searchString == null || searchString.isEmpty())
        return new GetRestaurantsResponse(new ArrayList<>());
      Double servingRadiusInKms = getServingRadiusInKms(currentTime);
      

     return new GetRestaurantsResponse(getOrderedRestaurantsListMt(latitude, longitude, searchString, currentTime, servingRadiusInKms));
  }

  private List<Restaurant> getOrderedRestaurantsListMt(Double latitude, Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> list1, list2, list3, list4;
    ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    Future<List<Restaurant>> list1Future = threadPool.submit(() -> 
      restaurantRepositoryService.findRestaurantsByName(latitude, longitude, searchString, currentTime, servingRadiusInKms)
    );
    
    Future<List<Restaurant>> list2Future = threadPool.submit(() -> 
    restaurantRepositoryService.findRestaurantsByAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms)
    );
    Future<List<Restaurant>> list3Future = threadPool.submit(() -> 
    restaurantRepositoryService.findRestaurantsByItemName(latitude, longitude, searchString, currentTime, servingRadiusInKms)
    );
    Future<List<Restaurant>> list4Future = threadPool.submit(() -> 
    restaurantRepositoryService.findRestaurantsByItemAttributes(latitude, longitude, searchString, currentTime, servingRadiusInKms)
    );

    try {
      list1 = list1Future.get();
      list2 = list2Future.get();
      list3 = list3Future.get();
      list4 = list4Future.get();

    } catch(Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);

    } finally {
      threadPool.shutdown();
    }

    return getOrderedRestaurantsList(list1, list2, list3, list4);

  }


}

