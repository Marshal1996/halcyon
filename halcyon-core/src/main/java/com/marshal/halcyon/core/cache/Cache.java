package com.marshal.halcyon.core.cache;

import java.util.List;

/**
 * @auth: Marshal
 * @date: 2019/6/25
 * @desc: 各种技术实现的缓存的抽象接口
 * 目前的实现有RedisCache
 */
public interface Cache<T> {

    /**
     * 缓存初始化加载
     */
    void init();

    /**
     * 缓存增删改查
     *
     * @param id
     * @return
     */
    T get(String id);

    List<T> getAll();

    void add(T t);

    void update(T t);

    void delete(Object id);

    /**
     * 缓存刷新
     */
    void refresh();

    /**
     * 清除缓存
     */
    void clear();


    /**
     * 缓存唯一标识
     *
     * @return
     */
    String getIdentifier();

}
