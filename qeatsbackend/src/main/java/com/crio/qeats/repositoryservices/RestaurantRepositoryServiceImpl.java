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
import java.util.Map;
import java.util.Objects;
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
  private MenuRepository menuRepository;

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

    List<RestaurantEntity> closeByRestaurantEntities = getCloseByRestaurantEntitiesFromCacheOrDB(key, latitude, longitude, servingRadiusInKms);
    return getOpenRestaurants(closeByRestaurantEntities, currentTime);

  }

  private List<RestaurantEntity> getCloseByRestaurantEntitiesFromCacheOrDB(String key, Double latitude, Double longitude, Double servingRadiusInKms) {
    try(Jedis jedis = redisConfiguration.getJedisPool().getResource()) {

      String fetchedJson = jedis.get(key);
      if(fetchedJson != null) {

        List<RestaurantEntity> cachedCloseByRestaurantEntities = objectMapper.readValue( fetchedJson, new TypeReference<List<RestaurantEntity>>() {});
        return cachedCloseByRestaurantEntities;
      }

      List<RestaurantEntity> dbCloseByRestaurantEntities = getCloseByRestaurantEntitiesFromDB(latitude, longitude, servingRadiusInKms);
      String json = objectMapper.writeValueAsString(dbCloseByRestaurantEntities);
      jedis.setex(key, RedisConfiguration.REDIS_ENTRY_EXPIRY_IN_SECONDS, json);

      return dbCloseByRestaurantEntities;

    } catch(Exception e) {
      e.printStackTrace();
      return Collections.emptyList();
    }

  }

  private List<RestaurantEntity> getCloseByRestaurantEntitiesFromDB(Double latitude, Double longitude, Double servingRadiusInKms) {
    List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();
    List<RestaurantEntity> nearByRestaurantEntities = getNearByRestaurantEntities(restaurantEntities, latitude, longitude, servingRadiusInKms);
    return nearByRestaurantEntities;
  }

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
   
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }

    return false;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
        List<RestaurantEntity> restaurantEntitiesWithMatchingName = restaurantRepository.findRestaurantsByNameExact(searchString);
        List<RestaurantEntity> nearByRestaurantEntitiesWithMatchingName = getNearByRestaurantEntities(restaurantEntitiesWithMatchingName, latitude, longitude, servingRadiusInKms);
        List<RestaurantEntity> sortedNearByRestaurantEntities = nearByRestaurantEntitiesWithMatchingName.stream()
        .sorted((r1, r2) -> {

          boolean exactNameMatchR1 = r1.getName().equalsIgnoreCase(searchString);
          boolean exactNameMatchR2 = r2.getName().equalsIgnoreCase(searchString);

          return Boolean.compare(exactNameMatchR2, exactNameMatchR1);
        })
        .collect(Collectors.toList());


     return getOpenRestaurants(sortedNearByRestaurantEntities, currentTime);
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
      List<RestaurantEntity> restaurantEntitiesWithMatchingAttribute = restaurantRepository.findByAttributesContainingIgnoreCase(searchString);
      List<RestaurantEntity> nearByRestaurantEntitiesWithMatchingAttribute = getNearByRestaurantEntities(restaurantEntitiesWithMatchingAttribute, latitude, longitude, servingRadiusInKms);

      return getOpenRestaurants(nearByRestaurantEntitiesWithMatchingAttribute, currentTime);
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
      //finding and sorting menu entities which have matching item name
      List<MenuEntity> menuEntitiesWithMatchingItemName = menuRepository.findMenusByItemsNameContainingIgnoreCase(searchString);
      List<String> orderedRestaurantIds = menuEntitiesWithMatchingItemName.stream()
      .sorted((m1, m2) -> {
        boolean isM1ContainsExactItem = isSearchStrEqualsAnyItemNameInMenuEntity(m1, searchString);
        boolean isM2ContainsExactItem = isSearchStrEqualsAnyItemNameInMenuEntity(m2, searchString);

        return Boolean.compare(isM2ContainsExactItem, isM1ContainsExactItem);
      })
      .map(MenuEntity::getRestaurantId)
      .distinct()
      .collect(Collectors.toList());
      //getting restaurants corresponding to founded matching menus
      List<RestaurantEntity> restaurantEntitiesWithMatchingItemName = restaurantRepository.findByRestaurantIdIn(orderedRestaurantIds);
      //ordering restaurant entity in same order as menu entity (with exact matches first)
      Map<String, RestaurantEntity> byIdMap = restaurantEntitiesWithMatchingItemName.stream()
      .collect(Collectors.toMap(RestaurantEntity::getRestaurantId, r -> r));
      List<RestaurantEntity> orderedRestaurantEntitiesWithMatchingItemName = orderedRestaurantIds.stream()
      .map(byIdMap::get)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
      //finding nearby reastaurants
      List<RestaurantEntity> nearByOrderedRestaurantEntitiesWithMatchingItemName = getNearByRestaurantEntities(orderedRestaurantEntitiesWithMatchingItemName, latitude, longitude, servingRadiusInKms);

     return getOpenRestaurants(nearByOrderedRestaurantEntitiesWithMatchingItemName, currentTime);
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

      List<MenuEntity> menuEntitiesWithMatchingItemAttribute = menuRepository.findMenusByItemsAttributesContainingIgnoreCase(searchString);
      List<String> restaurantIds = menuEntitiesWithMatchingItemAttribute.stream()
      .map(MenuEntity::getRestaurantId)
      .distinct()
      .collect(Collectors.toList());
      //getting restaurants corresponding to founded matching menus
      List<RestaurantEntity> restaurantEntitiesWithMatchingItemAttribute = restaurantRepository.findByRestaurantIdIn(restaurantIds);
      //finding nearby reastaurants
      List<RestaurantEntity> nearByRestaurantEntitiesWithMatchingItemAttribute = getNearByRestaurantEntities(restaurantEntitiesWithMatchingItemAttribute, latitude, longitude, servingRadiusInKms);

     return getOpenRestaurants(nearByRestaurantEntitiesWithMatchingItemAttribute, currentTime);
  }

  private boolean isSearchStrEqualsAnyItemNameInMenuEntity(MenuEntity menu, String searchString) {
    return menu.getItems().stream()
        .anyMatch(item -> item.getName().toLowerCase()
            .equals(searchString.toLowerCase()));
  }


}

