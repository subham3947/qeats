/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.models.RestaurantEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends MongoRepository<RestaurantEntity, String> {
  
  @Query("{'name':{'$regex':'?0','$options':'i'}}")  
  List<RestaurantEntity> findRestaurantEntityByNamePartial(String searchString);

  @Query("{'name':'?0'}")  
  Optional<List<RestaurantEntity>> findRestaurantsByNameExact(String searchString);

  //@Query(value = "{ 'attributes' : {$all : [?0] }}") 
  //@Query("{'categories': { $elemMatch: { a: { $regex: ?0, $options: 'i' }, b: ?1 } } }")
  @Query("{ 'attributes' : {$elemMatch: {$all : [?0], $regex: ?0, $options: 'i'}}}")
  List<RestaurantEntity> findRestaurantEntityByAttributes(String searchString);
  
  @Query("{restaurantId:{$in : ?0 }}")  
  List<Restaurant> findRestaurantEntityById(List<String> restaurantIdList);

    
}

