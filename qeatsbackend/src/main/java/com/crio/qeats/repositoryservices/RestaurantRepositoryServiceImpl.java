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
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Primary
@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private ModelMapper modelMapperProvider;

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
  // public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
  // Double longitude, LocalTime currentTime, Double servingRadiusInKms) {

  // List<Restaurant> restaurants = new ArrayList<Restaurant>();

  // List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();
  // for (RestaurantEntity restaurantEntity : restaurantEntities) {
  // if (isOpenNow(currentTime, restaurantEntity)
  // && isRestaurantCloseByAndOpen(restaurantEntity, currentTime,
  // latitude, longitude, servingRadiusInKms)) {
  // Restaurant res = modelMapperProvider.map(restaurantEntity, Restaurant.class);
  // restaurants.add(res);

  // }

  // }

  // CHECKSTYLE:OFF
  // CHECKSTYLE:ON

  // return restaurants;
  // }

  
  public String getGeohash(Double latitude, Double longitude) {
    GeoHash geoHash = GeoHash.withCharacterPrecision(20.0, 30.0, 7);
    return geoHash.toBase32();
  }

  // CHECKSTYLE:OFF
  // CHECKSTYLE:ON
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude, 
      LocalTime currentTime,Double servingRadiusInKms) {
    List<RestaurantEntity> restaurantEntities = null;
    JedisPool jedisPool = redisConfiguration.getJedisPool();
    try {
      Jedis jedis = jedisPool.getResource();
      String restaurantsJson = jedis.get(getGeohash(latitude, longitude)); 
      if (restaurantsJson == null) {
        restaurantEntities = setupCache(jedis, getGeohash(latitude, longitude));
      } else {
        restaurantEntities = new ObjectMapper().readValue(restaurantsJson,
          new TypeReference<List<RestaurantEntity>>() {});
      }
    } catch (Exception e) {
      restaurantEntities = restaurantRepository.findAll();
      System.out.print("Called");
    }
    List<Restaurant> restaurants = new ArrayList<Restaurant>();
    for (RestaurantEntity restaurantEntity : restaurantEntities) {
      if (isOpenNow(currentTime, restaurantEntity) 
          && isRestaurantCloseByAndOpen(restaurantEntity, currentTime,
          latitude, longitude, servingRadiusInKms)) {
        Restaurant res = modelMapperProvider.map(restaurantEntity, Restaurant.class);
        restaurants.add(res);
      }
    }
    return restaurants;
  }

  public List<RestaurantEntity> setupCache(Jedis jedis, String geoHash) throws IOException {
    List<RestaurantEntity> restaurantEntities = new ArrayList<RestaurantEntity>();
    restaurantEntities =  restaurantRepository.findAll();
    System.out.print("Called");
    try {
      String restaurantsJson = new ObjectMapper().writeValueAsString(restaurantEntities);
      jedis.set(geoHash, restaurantsJson);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }     
    return restaurantEntities;
  }

  

  // TODO: CRIO_TASK_MODULE_REDIS
  // We want to use cache to speed things up. Write methods that perform the same functionality,
  // but using the cache if it is present and reachable.
  // Remember, you must ensure that if cache is not present, the queries are directed at the
  // database instead.



  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objective:
  // 1. Check if a restaurant is nearby and open. If so, it is a candidate to be returned.
  // NOTE: How far exactly is "nearby"?

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

