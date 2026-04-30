package com.alicp.jetcache;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2017/5/24.
 *
 * @author huangli
 */
public class MultiLevelCacheConfig<K, V> extends CacheConfig<K, V> {
    private List<Cache<K, V>> caches = new ArrayList<>();
    private Boolean useExpireOfSubCache;

    @Override
    public MultiLevelCacheConfig clone() {
        MultiLevelCacheConfig copy = (MultiLevelCacheConfig) super.clone();
        if (caches != null) {
            copy.caches = new ArrayList(this.caches);
        }
        return copy;
    }

    public List<Cache<K, V>> getCaches() {
        return caches;
    }

    public void setCaches(List<Cache<K, V>> caches) {
        this.caches = caches;
    }

    @Deprecated
    public boolean isUseExpireOfSubCache() {
        return isUseExpireOfSubCacheEnabled();
    }

    @Deprecated
    public void setUseExpireOfSubCache(boolean useExpireOfSubCache) {
        this.useExpireOfSubCache = useExpireOfSubCache;
    }

    boolean isUseExpireOfSubCacheConfigured() {
        return useExpireOfSubCache != null;
    }

    boolean isUseExpireOfSubCacheEnabled() {
        return Boolean.TRUE.equals(useExpireOfSubCache);
    }
}
