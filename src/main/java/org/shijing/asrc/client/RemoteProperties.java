package org.shijing.asrc.client;


import org.shijing.asrc.client.model.DataDelta;
import org.shijing.asrc.client.model.IData;

import static org.shijing.asrc.client.model.DataDelta.DeltaType.ADDITION;
import static org.shijing.asrc.client.model.DataDelta.DeltaType.DELETION;
import static org.shijing.asrc.client.model.DataDelta.DeltaType.UPDATE;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class RemoteProperties {
    private static class Callback {
        final WeakReference<Object> observer;
        final Consumer<Map<String, DataDelta>> consumer;
        final Set<String> observeKeys;

        Callback(
                WeakReference<Object> observer,
                Consumer<Map<String, DataDelta>> consumer,
                Set<String> observeKeys) {
            this.observer = observer;
            this.consumer = consumer;
            this.observeKeys = observeKeys;
        }
    }

    private static IContextProvider CONTEXT_PROVIDER = null;
    private static IFetcher CONFIG_FETCHER = null;
    static final ConcurrentHashMap<String, IData> PROPERTIES_CACHE = new ConcurrentHashMap<>();
    static final ConcurrentLinkedQueue<Callback> LISTENERS = new ConcurrentLinkedQueue<>();
    static final ConcurrentHashMap<String, Boolean> OBSERVED_KEYS = new ConcurrentHashMap<>();

    /**
     * Set up Remote Property and
     *
     * @param contextProvider
     * @param fetcher
     * @return
     */
    public static CompletableFuture<Void> init(IContextProvider contextProvider, IFetcher fetcher) {
        CONTEXT_PROVIDER = contextProvider;
        CONFIG_FETCHER = fetcher;
        return updateContext(contextProvider.getContext());
    }

    /** Reset cache, listeners and observers. */
    static void reset() {
        PROPERTIES_CACHE.clear();
        LISTENERS.clear();
        OBSERVED_KEYS.clear();
    }

    /**
     * For each key in the lookupKeys, calculate the data change in {@code oldConfigs} and {@code
     * nowConfigs}.
     *
     * @param oldConfigs configs to which {@code nowConfigs} is compared
     * @param nowConfigs configs which is compared to the {@code oldConfigs}
     * @param lookupKeys a set of keys only within which the delta is calculated
     * @return deltas
     */
    private static Map<String, DataDelta> calculateDelta(
            Map<String, IData> oldConfigs, Map<String, IData> nowConfigs, Set<String> lookupKeys) {
        Map<String, DataDelta> deltas = new HashMap<>();
        for (String key : lookupKeys) {
            IData oldData = oldConfigs.get(key);
            IData nowData = nowConfigs.get(key);
            DataDelta.DeltaType deltaType;
            if (oldData == null && nowData == null) {
                // no-op in case of no change
            } else if (nowData == null) { // deletion case
                deltas.put(key, new DataDelta(oldData, nowData, DELETION));
            } else if (!nowData.equals(oldData)) { // addition or change case
                deltas.put(
                        key, new DataDelta(oldData, nowData, oldData == null ? ADDITION : UPDATE));
            }
            // else { skip the un-change case }
        }
        return deltas;
    }

    public static void updateConfigs(Map<String, IData> configs) {
        Map<String, IData> oldProperties = new HashMap<>(PROPERTIES_CACHE);

        PROPERTIES_CACHE.clear();
        PROPERTIES_CACHE.putAll(configs);

        Set<String> nowObservedKeys = new HashSet<>();
        Iterator<Callback> iterator = LISTENERS.iterator();
        while (iterator.hasNext()) {
            Callback callback = iterator.next();
            if (callback.observer.get() == null) {
                iterator.remove();
            } else {
                nowObservedKeys.addAll(callback.observeKeys);
                Map<String, DataDelta> deletas =
                        calculateDelta(oldProperties, configs, callback.observeKeys);
                if (!deletas.isEmpty()) {
                    callback.consumer.accept(deletas);
                }
            }
        }
        OBSERVED_KEYS.clear();
        nowObservedKeys.forEach(k -> OBSERVED_KEYS.put(k, true));
    }

    /**
     * Kick start of the whole client: 1. use fetcher to fetch the latest configurations. 2. exec
     * registered callbacks
     *
     * @param context
     */
    public static CompletableFuture<Void> updateContext(Map<String, IData> context) {
        if (CONFIG_FETCHER != null) {
            return CONFIG_FETCHER.fetch(context).thenAcceptAsync(RemoteProperties::updateConfigs);
        }
        return CompletableFuture.completedFuture(null);
    }

    public static void setFetcher(IFetcher fetcher) {
        CONFIG_FETCHER = fetcher;
    }

    /**
     * Observe on changes to configurations of `observedKeys`, can exec callback if and only if
     * the configuration value is changed.
     *
     * Be noted that this method does not check for duplicates, if one callback is registered multiple times,
     * it will be called multiple times as well when new configs are received.
     *
     * @param object (Nonnull) anchor  that all data consumers are registered with. It is used for
     *                 callback clean up:
     *                 1. The objectReference holds an observer object which if is gc-ed, all its associated
     *                 callbacks will be invalidated and cleaned.
     *                 2. When {@code removeListeners is called, all registered callbacks will be removed.
     * @param observedKeys
     * @param dataConsumer
     */
    public static synchronized void addListener(
            Object object,
            Set<String> observedKeys,
            Consumer<Map<String, DataDelta>> dataConsumer) {

        removeListeners(null);
        Map<String, IData> configs = new HashMap<>();
        for (String key : observedKeys) {
            OBSERVED_KEYS.put(key, true);
            IData data = PROPERTIES_CACHE.get(key);
            if (data != null) {
                configs.put(key, data);
            }
        }
        if (configs.size() > 0) {
            dataConsumer.accept(calculateDelta(Collections.emptyMap(), configs, observedKeys));
        }
        LISTENERS.add(new Callback(new WeakReference<>(object), dataConsumer, observedKeys));
    }

    /**
     * Remove listeners that are associated with the {@code objectReference}.
     *
     * @param object the observer to remove, or null if we went to purge dead listeners.
     */
    public static synchronized void removeListeners(Object object) {
        Set<String> nowObservedKeys = new HashSet<>();
        Iterator<Callback> iter = LISTENERS.iterator();
        while (iter.hasNext()) {
            Callback callback = iter.next();
            Object stored = callback.observer.get();
            if (stored == null || stored == object) {
                iter.remove();
            } else {
                nowObservedKeys.addAll(callback.observeKeys);
            }
        }
        OBSERVED_KEYS.clear();
        nowObservedKeys.forEach(k -> OBSERVED_KEYS.put(k, true));
    }

    public static synchronized void removeAllListeners() {
        LISTENERS.clear();
    }

    public static Optional<Integer> getInt(String key) {
        return Optional.ofNullable(PROPERTIES_CACHE.get(key)).map(IData::toInt);
    }

    public static Integer getIntOrDefault(String key, Integer defaultValue) {
        return getInt(key).orElse(defaultValue);
    }

    public static Optional<Double> getDouble(String key) {
        return Optional.ofNullable(PROPERTIES_CACHE.get(key)).map(IData::toDouble);
    }

    public static Double getDoubleOrDefault(String key, Double defaultValue) {
        return getDouble(key).orElse(defaultValue);
    }

    public static Optional<Boolean> getBoolean(String key) {
        return Optional.ofNullable(PROPERTIES_CACHE.get(key)).map(IData::toBool);
    }

    public static Boolean getBooleanOrDefault(String key, Boolean defaultValue) {
        return getBoolean(key).orElse(defaultValue);
    }

    public static Optional<String> getString(String key) {
        return Optional.ofNullable(PROPERTIES_CACHE.get(key)).map(IData::toString);
    }

    public static String getStringOrDefault(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }
}
