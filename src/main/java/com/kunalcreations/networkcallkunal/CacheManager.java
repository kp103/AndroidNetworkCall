package com.kunalcreations.networkcallkunal;

import android.content.Context;
import android.util.LruCache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;

public class CacheManager<T> {

    private static final int MEMORY_CACHE_SIZE = 20; // Define memory cache size
    private static final LruCache<String, Object> memoryCache = new LruCache<>(MEMORY_CACHE_SIZE); // In-memory cache
    private final File cacheDir;
    private final Gson gson;

    public CacheManager(Context context) {
        this.cacheDir = new File(context.getCacheDir(), "api_responses");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        this.gson = new Gson(); // Initialize Gson for (de)serialization
    }

    // Save data to cache
    public void saveData(String key, T data) {
        // Save to in-memory cache
        memoryCache.put(key, data);

        // Also save to disk cache
        File cacheFile = new File(cacheDir, generateFileName(key));
        try (FileWriter writer = new FileWriter(cacheFile)) {
            writer.write(gson.toJson(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Retrieve data from cache (first tries in-memory, then disk)
    public T getData(Class<T> clazz, String key) {
        // First try to retrieve data from in-memory cache
        T cachedData = (T) memoryCache.get(key);
        if (cachedData != null) {
            return cachedData;
        }

        // If not found in memory, try retrieving from disk
        File cacheFile = new File(cacheDir, generateFileName(key));
        if (cacheFile.exists()) {
            try (FileReader reader = new FileReader(cacheFile)) {
                Type type = TypeToken.get(clazz).getType();
                return gson.fromJson(reader, type); // Deserialize from JSON
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null; // If no cached data is found
    }

    // Delete cached data
    public void deleteData(String key) {
        // Remove from memory cache
        memoryCache.remove(key);

        // Also remove from disk
        File cacheFile = new File(cacheDir, generateFileName(key));
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    // Clear all cached data
    public void clearCache() {
        // Clear memory cache
        memoryCache.evictAll();

        // Clear disk cache
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    // Generate a file name for caching based on key
    private String generateFileName(String key) {
        return key.hashCode() + ".json"; // Hash key to generate a unique file name
    }
}
