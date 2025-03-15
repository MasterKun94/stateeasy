package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.Snapshot;
import io.masterkun.stateeasy.core.StateDef;
import io.masterkun.stateeasy.core.StateStore;
import io.masterkun.stateeasy.indexlogging.EventLogger;
import io.masterkun.stateeasy.indexlogging.IdAndOffset;
import io.masterkun.stateeasy.indexlogging.LogSystem;
import io.masterkun.stateeasy.indexlogging.Serializer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LocalFileStateStore<STATE> implements StateStore<STATE> {

    private final LogFileEventStoreConfig config;
    private final Serializer<Snapshot<STATE>> serializer;
    private EventLogger<Snapshot<STATE>> logger;

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
                listener.success(null);
            }

            @Override
            public void failure(Throwable cause) {
                listener.failure(cause);
            }
        });
    }

    @Override
    public void read(EventStageListener<SnapshotAndId<STATE>> listener) {
        long endId = logger.endId();
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

    }

    @Override
    public void close() {

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
