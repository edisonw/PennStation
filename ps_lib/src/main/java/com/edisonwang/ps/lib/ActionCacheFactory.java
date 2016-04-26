package com.edisonwang.ps.lib;

/**
 * @author edi
 */
public interface ActionCacheFactory {

    ActionCache getCache(FullAction action, FullAction.CachePolicy policy);
}
