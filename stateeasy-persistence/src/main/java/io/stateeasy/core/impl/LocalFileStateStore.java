package io.stateeasy.core.impl;

import io.stateeasy.concurrent.EventStageListener;
import io.stateeasy.core.Snapshot;
import io.stateeasy.core.SnapshotAndId;
import io.stateeasy.core.StateDef;
import io.stateeasy.core.StateStore;
import io.stateeasy.indexlogging.EventLogger;
import io.stateeasy.indexlogging.IdAndOffset;
import io.stateeasy.indexlogging.LogSystem;
import io.stateeasy.indexlogging.Serializer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A concrete implementation of the {@link StateStore} interface that persists state snapshots to
 * local files. This class leverages a logging system for efficient and reliable storage of state
 * data.
 *
 * @param <STATE> the type of the state being managed by the state store
 */
public class LocalFileStateStore<STATE> implements StateStore<STATE>, Closeable {

    private final LogFileEventStoreConfig config;
    private final Serializer<Snapshot<STATE>> serializer;
    @VisibleForTesting
    EventLogger<Snapshot<STATE>> logger;

    public LocalFileStateStore(LogFileEventStoreConfig config,
                               Serializer<STATE> serializer) {
        this.config = config;
        this.serializer = new SnapshotSerializer<>(serializer);
    }

    @Override
    public void initialize(StateDef<STATE, ?> stateDef, EventStageListener<Void> listener) {
        try {
            LogSystem system =
                    LocalFileLogSystemProvider.getLogSystem(config.getThreadNumPerDisk());
            String name = "stage-store-" + stateDef.name();
            this.logger = system.get(config.toLogConfig(name), serializer);

            listener.success(null);
        } catch (Throwable e) {
            listener.failure(e);
        }
    }

    @Override
    public void write(Snapshot<STATE> snapshot, EventStageListener<Long> listener) {
        logger.write(snapshot, true, new EventStageListener<>() {
            @Override
            public void success(IdAndOffset value) {
                listener.success(value.id());
            }

            @Override
            public void failure(Throwable cause) {
                listener.failure(cause);
            }
        });
    }

    @Override
    public void read(EventStageListener<@Nullable SnapshotAndId<STATE>> listener) {
        long endId = logger.endId();
        if (endId == -1) {
            listener.success(null);
            return;
        }
        logger.readOne(endId, new EventStageListener<>() {
            @Override
            public void success(Snapshot<STATE> value) {
                listener.success(new SnapshotAndId<>(endId, value));
            }

            @Override
            public void failure(Throwable cause) {
                listener.failure(cause);
            }
        });
    }

    @Override
    public void expire(long expireBeforeSnapshotId, EventStageListener<Boolean> listener) {
        logger.expire(expireBeforeSnapshotId, listener);
    }

    @Override
    public void close() throws IOException {
        LocalFileLogSystemProvider.getLogSystem(1)
                .closeLogger(logger);
        logger = null;
    }

    private static class SnapshotSerializer<STATE> implements Serializer<Snapshot<STATE>> {
        private final Serializer<STATE> stateSerializer;

        private SnapshotSerializer(Serializer<STATE> stateSerializer) {
            this.stateSerializer = stateSerializer;
        }

        @Override
        public void serialize(Snapshot<STATE> obj, DataOut out) throws IOException {
            out.writeLong(obj.eventId());
            stateSerializer.serialize(obj.state(), out);
            int size = obj.metadata().size();
            out.writeInt(size);
            if (size > 0) {
                for (Map.Entry<String, String> entry : obj.metadata().entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }
            }
        }

        @Override
        public Snapshot<STATE> deserialize(DataIn in) throws IOException {
            long eventId = in.readLong();
            STATE state = stateSerializer.deserialize(in);
            int size = in.readInt();
            Map<String, String> metadata;
            if (size == 0) {
                metadata = Map.of();
            } else {
                metadata = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    metadata.put(in.readUTF(), in.readUTF());
                }
            }
            return new Snapshot<>(state, eventId,
                    Collections.unmodifiableMap(metadata));
        }
    }
}
