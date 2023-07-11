
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    List<Restaurant> restaurant = new ArrayList<>();
    int hour = currentTime.getHour();
    restaurant = restaurantRepositoryService.findAllRestaurantsCloseBy(
        getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), currentTime, 
        findRadius(hour));   
    
    
    GetRestaurantsResponse getRestaurantsResponse = new GetRestaurantsResponse(restaurant);
    log.info(getRestaurantsResponse);
    return getRestaurantsResponse;
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
    Set<Restaurant> restaurantSet = new LinkedHashSet<Restaurant>();
    int hour = currentTime.getHour();
    if (getRestaurantsRequest.getSearchFor() != "") {
      restaurantSet.addAll(restaurantRepositoryService
          .findRestaurantsByName(getRestaurantsRequest.getLatitude(), 
          getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), 
          currentTime, findRadius(hour)));
      restaurantSet.addAll(restaurantRepositoryService
          .findRestaurantsByAttributes(getRestaurantsRequest.getLatitude(), 
          getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), 
          currentTime, findRadius(hour)));
      restaurantSet.addAll(restaurantRepositoryService
          .findRestaurantsByItemName(getRestaurantsRequest.getLatitude(), 
          getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), 
          currentTime, findRadius(hour)));
      restaurantSet.addAll(restaurantRepositoryService
          .findRestaurantsByItemAttributes(getRestaurantsRequest.getLatitude(), 
          getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), 
          currentTime, findRadius(hour)));
    }
    
    return new GetRestaurantsResponse(new ArrayList<Restaurant>(restaurantSet));
  }

  public Double findRadius(int hour) {
    if ((hour >= 8 && hour <= 10) || (hour >= 13 && hour <= 14) || (hour >= 19 && hour <= 21)) {
      return peakHoursServingRadiusInKms;
    } else {
      return normalHoursServingRadiusInKms;
    }
  }

}

