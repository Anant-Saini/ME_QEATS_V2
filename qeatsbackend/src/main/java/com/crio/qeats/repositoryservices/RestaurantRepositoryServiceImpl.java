/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;


@Service
@Primary
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {



  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private RestaurantRepository restaurantRepository;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
      Double longitude, LocalTime currentTime, Double servingRadiusInKms) {

    int precision = servingRadiusInKms == 3.0 ? 7 : 6;
    GeoHash geohash = GeoHash.withCharacterPrecision(latitude, longitude, precision);
    String key = geohash.toBase32();
    
    try(Jedis jedis = redisConfiguration.getJedisPool().getResource()) {

      String fetchedJson = jedis.get(key);
      if(fetchedJson != null && !fetchedJson.isEmpty()) {

        List<RestaurantEntity> cachedRestaurantEntities = objectMapper.readValue( fetchedJson, new TypeReference<List<RestaurantEntity>>() {});
        return getOpenRestaurants(cachedRestaurantEntities, currentTime);
      }

      List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();
      List<RestaurantEntity> nearByRestaurantEntities = getNearByRestaurantEntities(restaurantEntities, latitude, longitude, servingRadiusInKms);
      String json = objectMapper.writeValueAsString(nearByRestaurantEntities);
      jedis.setex(key, RedisConfiguration.REDIS_ENTRY_EXPIRY_IN_SECONDS, json);

      return getOpenRestaurants(nearByRestaurantEntities, currentTime);


    } catch(Exception e) {
      e.printStackTrace();
      return Collections.emptyList();
    }

  }
  // public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
  //     Double longitude, LocalTime currentTime, Double servingRadiusInKms) {

  //   List<Restaurant> restaurants = null;
  //   // TODO: CRIO_TASK_MODULE_REDIS
  //   // We want to use cache to speed things up. Write methods that perform the same functionality,
  //   // but using the cache if it is present and reachable.
  //   // Remember, you must ensure that if cache is not present, the queries are directed at the
  //   // database instead.


  //     //CHECKSTYLE:OFF
  //     //CHECKSTYLE:ON


  //   return restaurants;
  // }
  private List<Restaurant> getOpenRestaurants(List<RestaurantEntity> restaurantEntities, LocalTime currentTime) {
    
    return restaurantEntities.stream().filter( entity -> isOpenNow(currentTime, entity) )
    .map( entity -> modelMapperProvider.get().map(entity, Restaurant.class))
    .collect(Collectors.toList());
  }

  private List<RestaurantEntity> getNearByRestaurantEntities(List<RestaurantEntity> restaurantEntities, Double latitude, Double longitude, Double servingRadiusInKms) {
    return restaurantEntities.stream()
    .filter( entity -> GeoUtils.findDistanceInKm(latitude, longitude, entity.getLatitude(), entity.getLongitude()) < servingRadiusInKms)
    .collect(Collectors.toList());

  }


  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
      Double longitude, LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurants = null;




    return restaurants;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {


     return null;
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {


     return null;
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {


     return null;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

     return null;
  }





  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }

    return false;
  }



}

