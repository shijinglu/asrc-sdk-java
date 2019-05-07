package org.shijing.asrc.client;

import org.shijing.asrc.client.model.BoolData;
import org.shijing.asrc.client.model.DoubleData;
import org.shijing.asrc.client.model.IData;
import org.shijing.asrc.client.model.IntData;
import org.shijing.asrc.client.model.StringData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TestModule {
    static final String CONFIG_BOOL_TRUE = "config_bool_true";
    static final String CONFIG_BOOL_FALSE = "config_bool_false";
    static final String CONFIG_INT_123 = "config_ing_123";
    static final String CONFIG_DOUBLE_314 = "config_double_314";
    static final String CONFIG_STRING_ALICE = "config_string_alice";

    static final Map<String, IData> CONFIGS_FIXTURE =
            new HashMap<String, IData>() {
                {
                    put(CONFIG_BOOL_TRUE, BoolData.TRUE);
                    put(CONFIG_BOOL_FALSE, BoolData.FALSE);
                    put(CONFIG_INT_123, new IntData(123));
                    put(CONFIG_DOUBLE_314, new DoubleData(3.14));
                    put(CONFIG_STRING_ALICE, new StringData("alice"));
                }
            };

    static IFetcher provideFetcher() {
        return new IFetcher() {
            @Override
            public CompletableFuture<Map<String, IData>> fetch(Map<String, IData> context) {
                return CompletableFuture.completedFuture(CONFIGS_FIXTURE);
            }
        };
    }

    static IContextProvider provideContextProvider() {
        return new IContextProvider() {
            @Override
            public Map<String, IData> getContext() {
                return Collections.emptyMap();
            }
        };
    }
}
