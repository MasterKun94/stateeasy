package io.stateeasy.indexlogging.executor;

import org.junit.Test;

public class EventExecutorFactoryTest {

    @Test
    public void test() {
        for (EventExecutorFactory executorFactory : EventExecutorFactory.ALL_INSTANCE) {
            System.out.println(executorFactory);
        }
    }

}
