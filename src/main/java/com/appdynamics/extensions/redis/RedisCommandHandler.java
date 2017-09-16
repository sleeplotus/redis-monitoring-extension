/**
 * Copyright 2017 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.redis;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.redis.metrics.RedisMetrics;
import com.appdynamics.extensions.redis.metrics.SlowLogMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

class RedisCommandHandler {
    private MonitorConfiguration configuration;
    private Map<String, String> server;
    private JedisPool jedisPool;
    private Logger logger = LoggerFactory.getLogger(RedisCommandHandler.class);
    private CountDownLatch countDownLatch;
    private long previousTimeStamp;
    private long currentTimeStamp;


    RedisCommandHandler(MonitorConfiguration configuration, Map<String, String> server, JedisPool jedisPool, long previousTimeStamp, long currentTimeStamp) {
        this.configuration = configuration;
        this.server = server;
        this.jedisPool = jedisPool;
        countDownLatch = new CountDownLatch(2);
        this.previousTimeStamp = previousTimeStamp;
        this.currentTimeStamp = currentTimeStamp;
    }

     void parseMap() {
        SlowLogMetrics slowLogMetricsTask = new SlowLogMetrics(configuration, server, jedisPool, countDownLatch, previousTimeStamp, currentTimeStamp);
        configuration.getExecutorService().execute("RedisSlowLogMonitorTask",slowLogMetricsTask);
        RedisMetrics redisMetricsTask = new RedisMetrics(configuration, server, jedisPool, countDownLatch);
        configuration.getExecutorService().execute("RedisMetricsExtractionTask",redisMetricsTask);
        try{
            countDownLatch.await();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
        finally {
            jedisPool.destroy();
        }

    }
}
