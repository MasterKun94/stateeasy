package io.masterkun.stateeasy.core;

import java.util.HashMap;
import java.util.Map;

public class StateManagerTestKit {

    public static class TestState {
        private final Map<String, String> data = new HashMap<>();

        public void put(SnapshotStateManagerTest.TestEvent event) {
            data.put(event.key(), event.value());
        }

        public TestState merge(TestState inc) {
            data.putAll(inc.data);
            return this;
        }

        public String get(String key) {
            return data.get(key);
        }

        public int size() {
            return data.size();
        }
    }
}
