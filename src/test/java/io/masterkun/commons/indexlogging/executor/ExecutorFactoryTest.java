package io.masterkun.commons.indexlogging.executor;

import org.junit.Test;

public class ExecutorFactoryTest {

    @Test
    public void test() {
        for (ExecutorFactory executorFactory : ExecutorFactory.ALL_INSTANCE) {
            System.out.println(executorFactory);
        }
    }

}
