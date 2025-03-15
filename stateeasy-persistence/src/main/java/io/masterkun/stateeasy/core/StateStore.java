package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventStageListener;
import org.jetbrains.annotations.Nullable;

/**
 * A generic interface for a state store, which is responsible for periodically persisting in-memory
 * state data and for data recovery. The state store provides methods to initialize, write, read,
 * and expire state snapshots.
 *
 * @param <STATE> the type of the state being managed by the state store
 */
public interface StateStore<STATE> {

    /**
     * Initializes the state store with the given state definition and event stage listener.
     *
     * @param stateDef the state definition that provides initial state, update logic, and snapshot
     *                 configuration
     * @param listener the event stage listener to be notified upon the completion of the
     *                 initialization process
     */
    void initialize(StateDef<STATE, ?> stateDef, EventStageListener<Void> listener);

    /**
     * Writes a state snapshot to the persistent storage.
     *
     * @param snapshot the snapshot containing the state, event ID, and metadata to be written
     * @param listener the event stage listener to be notified upon the completion of the write
     *                 operation, either with the event ID on success or with an error
     */
    void write(Snapshot<STATE> snapshot, EventStageListener<Long> listener);

    /**
     * Reads the most recent state snapshot from the persistent storage and notifies the provided
     * listener with the result.
     *
     * @param listener the event stage listener to be notified upon the completion of the read
     *                 operation, either with a {@code SnapshotAndId<STATE>} on success or with an
     *                 error. The {@code SnapshotAndId<STATE>} may be null if no snapshot is
     *                 available.
     */
    void read(EventStageListener<@Nullable SnapshotAndId<STATE>> listener);

    /**
     * Expires state snapshots that have an event ID less than the specified
     * {@code expireBeforeSnapshotId}. This method is asynchronous and will notify the provided
     * listener upon completion.
     *
     * @param expireBeforeSnapshotId the event ID before which all snapshots should be expired
     * @param listener               the event stage listener to be notified upon the completion of
     *                               the expiration process, with a boolean value indicating success
     *                               or failure
     */
    void expire(long expireBeforeSnapshotId, EventStageListener<Boolean> listener);
}
