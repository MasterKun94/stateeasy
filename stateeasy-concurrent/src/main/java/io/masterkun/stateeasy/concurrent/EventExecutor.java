package io.masterkun.stateeasy.concurrent;

import java.util.concurrent.ScheduledExecutorService;

public sealed interface EventExecutor extends ScheduledExecutorService
        permits SingleThreadEventExecutor {
    boolean inExecutor();

}
