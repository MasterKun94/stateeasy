package io.masterkun.stateeasy.core;

public interface IncrementalSnapshotStateDef<STATE, EVENT> extends StateDef<STATE, EVENT> {
    IncrementalSnapshotConfig incrementalSnapshotConfig();

    STATE merge(STATE previous, STATE incremental);
}
