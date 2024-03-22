package com.alicp.jetcache.autoconfigure;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.alicp.jetcache.embedded.EmbeddedCacheConfig;
import com.alicp.jetcache.redis.RedisCacheConfig;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheTest;
import com.alicp.jetcache.test.beans.MyFactoryBean;
import com.alicp.jetcache.test.spring.SpringTest;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.util.Pool;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * Created on 2016/11/23.
 *
 * @author huangli
 */
@Configuration
@EnableAutoConfiguration(exclude = RedissonAutoConfigurationV2.class)
@ComponentScan(basePackages = {"com.alicp.jetcache.test.beans", "com.alicp.jetcache.anno.inittestbeans"})
@EnableMethodCache(basePackages = {"com.alicp.jetcache.test.beans", "com.alicp.jetcache.anno.inittestbeans"})
@EnableCreateCacheAnnotation
public class RedisStarterTest extends SpringTest {

    @Test
    public void tests() throws Exception {
        if (RedisLettuceCacheTest.checkOS()) {
            System.setProperty("spring.profiles.active", "redis-cluster");
        } else {
            System.setProperty("spring.profiles.active", "redis");
        }
        context = SpringApplication.run(RedisStarterTest.class);
        doTest();
        A bean = context.getBean(A.class);
        bean.test();

        Pool<Jedis> t1 = (Pool<Jedis>) context.getBean("defaultPool");
        Pool<Jedis> t2 = (Pool<Jedis>) context.getBean("A1Pool");
        Assert.assertTrue(t1 instanceof Pool);
        Assert.assertTrue(t2 instanceof JedisSentinelPool);
        Assert.assertNotSame(t1, t2);

        if (RedisLettuceCacheTest.checkOS()) {
            JedisCluster a2 = (JedisCluster) context.getBean("A2Jedis");
            Assert.assertNotNull(a2);
        }
    }

    @Component
    public static class A {
        @CreateCache(cacheType = CacheType.LOCAL)
        private Cache c1;

        @CreateCache
        private Cache c2;

        public void test() {
            Assert.assertNotNull(c1.unwrap(com.github.benmanes.caffeine.cache.Cache.class));
            EmbeddedCacheConfig cc1 = (EmbeddedCacheConfig) c1.config();
            Assert.assertEquals(200, cc1.getLimit());
            Assert.assertEquals(10000, cc1.getExpireAfterWriteInMillis());
            Assert.assertFalse(cc1.isExpireAfterAccess());

            RedisCacheConfig c = (RedisCacheConfig) c2.config();
            Assert.assertFalse(c.isReadFromSlave());
            Pool[] slavePools = c.getJedisSlavePools();
            Assert.assertEquals(2, slavePools.length);
            int[] ws = c.getSlaveReadWeights();
            Assert.assertTrue(Arrays.equals(new int[]{30, 100}, ws) || Arrays.equals(new int[]{100, 30}, ws));
        }
    }

    @Component
    public static class B {
        @Autowired
        private Pool<Jedis> defaultPool;

        @Autowired
        private Pool<Jedis> A1Pool;

        @PostConstruct
        public void init() {
            Assert.assertNotNull(defaultPool);
            Assert.assertNotNull(A1Pool);
        }
    }

    @Configuration
    public static class Config {
        @Bean(name = "factoryBeanTarget")
        public MyFactoryBean factoryBean() {
            return new MyFactoryBean();
        }

        @Bean(name = "defaultPool")
        @DependsOn(RedisAutoConfiguration.AUTO_INIT_BEAN_NAME)
        public JedisPoolFactory defaultPool() {
            return new JedisPoolFactory("remote.default", JedisPool.class);
        }

        @Bean(name = "A1Pool")
        @DependsOn(RedisAutoConfiguration.AUTO_INIT_BEAN_NAME)
        public JedisPoolFactory A1Pool() {
            return new JedisPoolFactory("remote.A1", JedisSentinelPool.class);
        }

        @Bean(name = "A2Jedis")
        @DependsOn(RedisAutoConfiguration.AUTO_INIT_BEAN_NAME)
        public JedisFactory A2Jedis() {
            return new JedisFactory("remote.A2", JedisCluster.class);
        }
    }

}
