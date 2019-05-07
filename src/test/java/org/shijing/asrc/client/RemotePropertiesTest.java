package org.shijing.asrc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.shijing.asrc.client.model.DataDelta.DeltaType.ADDITION;
import static org.shijing.asrc.client.model.DataDelta.DeltaType.DELETION;
import static org.shijing.asrc.client.model.DataDelta.DeltaType.UPDATE;
import static org.shijing.asrc.client.TestModule.CONFIGS_FIXTURE;
import static org.shijing.asrc.client.TestModule.CONFIG_BOOL_FALSE;
import static org.shijing.asrc.client.TestModule.CONFIG_BOOL_TRUE;
import static org.shijing.asrc.client.TestModule.CONFIG_DOUBLE_314;
import static org.shijing.asrc.client.TestModule.CONFIG_INT_123;
import static org.shijing.asrc.client.TestModule.CONFIG_STRING_ALICE;
import static org.shijing.asrc.client.TestModule.provideContextProvider;
import static org.shijing.asrc.client.TestModule.provideFetcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.shijing.asrc.client.model.BoolData;
import org.shijing.asrc.client.model.DataDelta;
import org.shijing.asrc.client.model.DoubleData;
import org.shijing.asrc.client.model.IData;
import org.shijing.asrc.client.model.IntData;
import org.shijing.asrc.client.model.StringData;

public class RemotePropertiesTest {

    private final String configStringBob = "config_string_bob";
    private final Set<String> keys =
            new HashSet<>(
                    Arrays.asList(
                            CONFIG_BOOL_TRUE,
                            CONFIG_INT_123,
                            CONFIG_DOUBLE_314,
                            CONFIG_STRING_ALICE,
                            configStringBob));

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        RemoteProperties.reset();
        RemoteProperties.init(provideContextProvider(), provideFetcher()).get();
    }

    @Test
    public void testGetters() {
        assertEquals(RemoteProperties.getBoolean(CONFIG_BOOL_TRUE).get(), true);
        assertEquals(RemoteProperties.getBoolean(CONFIG_BOOL_FALSE).get(), false);
        assertEquals(RemoteProperties.getString(CONFIG_STRING_ALICE).get(), "alice");
        assertEquals(RemoteProperties.getDouble(CONFIG_DOUBLE_314).get(), Double.valueOf(3.14));
        assertEquals(RemoteProperties.getInt(CONFIG_INT_123).get(), Integer.valueOf(123));

        assertEquals(RemoteProperties.getBooleanOrDefault("test", true), true);
        assertEquals(RemoteProperties.getIntOrDefault("test", 234), Integer.valueOf(234));
        assertEquals(RemoteProperties.getDoubleOrDefault("test", 1.2345), Double.valueOf(1.2345));
        assertEquals(RemoteProperties.getStringOrDefault("test", "xyz"), "xyz");
    }

    @Test
    public void testAddListenersTriggerCallback() {

        Object obj = new Object();

        AtomicInteger calledTimes = new AtomicInteger(0);

        AtomicReference<Map<String, DataDelta>> expectedDataRef =
                new AtomicReference<>(
                        new HashMap<String, DataDelta>() {
                            {
                                put(CONFIG_BOOL_TRUE, new DataDelta(null, BoolData.TRUE, ADDITION));
                                put(
                                        CONFIG_INT_123,
                                        new DataDelta(null, new IntData(123), ADDITION));
                                put(
                                        CONFIG_DOUBLE_314,
                                        new DataDelta(null, new DoubleData(3.14), ADDITION));
                                put(
                                        CONFIG_STRING_ALICE,
                                        new DataDelta(null, new StringData("alice"), ADDITION));
                            }
                        });

        RemoteProperties.addListener(
                new WeakReference<>(obj),
                keys,
                deltaMap -> {
                    assertEquals(deltaMap.size(), expectedDataRef.get().size());
                    expectedDataRef
                            .get()
                            .forEach((key, value) -> assertEquals(deltaMap.get(key), value));

                    calledTimes.incrementAndGet();
                });
        assertEquals(calledTimes.get(), 1);

        assertTrue(RemoteProperties.OBSERVED_KEYS.containsKey(CONFIG_BOOL_TRUE));
        assertTrue(RemoteProperties.OBSERVED_KEYS.containsKey(CONFIG_INT_123));
        assertTrue(RemoteProperties.OBSERVED_KEYS.containsKey(CONFIG_DOUBLE_314));
        assertTrue(RemoteProperties.OBSERVED_KEYS.containsKey(CONFIG_STRING_ALICE));

        // case 1: update the config with the same configs will not trigger update
        RemoteProperties.updateConfigs(CONFIGS_FIXTURE);
        assertEquals(calledTimes.get(), 1);

        // case 2: update the config with different set of configs triggers the callbacks
        expectedDataRef.set(
                new HashMap<String, DataDelta>() {
                    {
                        put(CONFIG_BOOL_TRUE, new DataDelta(BoolData.TRUE, null, DELETION));
                        put(
                                CONFIG_INT_123,
                                new DataDelta(new IntData(123), new IntData(234), UPDATE));
                        put(configStringBob, new DataDelta(null, new StringData("bob"), ADDITION));
                    }
                });

        RemoteProperties.updateConfigs(
                new HashMap<String, IData>() {
                    {
                        // put(CONFIG_BOOL_TRUE, BoolData.TRUE);   // deletion and being observed
                        // put(CONFIG_BOOL_FALSE, BoolData.FALSE); // deletion but not being
                        // observed
                        put(CONFIG_INT_123, new IntData(234)); // update and being observed
                        put(
                                CONFIG_DOUBLE_314,
                                new DoubleData(3.14)); // being observed but unchanged
                        put(
                                CONFIG_STRING_ALICE,
                                new StringData("alice")); // being observed but unchanged
                        put(configStringBob, new StringData("bob")); // new value and being observed
                    }
                });

        assertEquals(calledTimes.get(), 2);
        // reset
        RemoteProperties.reset();
    }

    private void subTestAddListenersManagement(AtomicInteger calledTimes) {
        Map<String, String> obj1 = new HashMap<>();
        List<String> obj2 = new ArrayList<>();

        RemoteProperties.addListener(obj1, keys, deltaMap -> calledTimes.incrementAndGet());
        RemoteProperties.addListener(obj2, keys, deltaMap -> calledTimes.incrementAndGet());
        assertEquals(calledTimes.get(), 2);

        RemoteProperties.removeListeners(obj1);
        RemoteProperties.updateConfigs(Collections.emptyMap());
        assertEquals(calledTimes.get(), 3); // now only one listener after removal
    }

    @Test
    public void testAddListenersManagement() {
        AtomicInteger calledTimes = new AtomicInteger(0);
        subTestAddListenersManagement(calledTimes);
        System.gc(); // obj1 and obj2 should be gc-ed
        RemoteProperties.updateConfigs(CONFIGS_FIXTURE);
        assertEquals(calledTimes.get(), 3); // second listener is deactivated.
    }
}
