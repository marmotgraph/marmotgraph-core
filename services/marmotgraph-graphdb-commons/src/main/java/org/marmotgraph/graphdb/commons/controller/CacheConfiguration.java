/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.graphdb.commons.controller;

import org.marmotgraph.commons.cache.CacheConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import java.time.Duration;

@Configuration
public class CacheConfiguration {
    @Value("${org.marmotgraph.cache.ttl:30}")
    private long ttlInMinutes;

    @Value("${org.marmotgraph.cache.enabled:false}")
    private boolean sharedCacheEnabled;

    @Value("${spring.application.name}")
    private String applicationName;

    private static final String SEPARATOR = ":";

    // In-memory cache
    @Bean(name = CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public CacheManager inMemoryCacheManager() {
        return new ConcurrentMapCacheManager(
                sharedCacheEnabled ? CacheConstant.CACHE_KEYS_IN_MEMORY : CacheConstant.CACHE_KEYS_ALL
        );
    }

    @Primary
    @Bean
    @ConditionalOnProperty(value = "org.marmotgraph.cache.enabled", havingValue = "true", matchIfMissing = false)
    public CacheManager redisCacheManager(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlInMinutes))
                .computePrefixWith(cacheName -> applicationName.concat(SEPARATOR)
                        .concat(cacheName).concat(SEPARATOR))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));
        redisCacheConfiguration.usePrefix();

        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(lettuceConnectionFactory)
                .cacheDefaults(redisCacheConfiguration).build();
    }
}