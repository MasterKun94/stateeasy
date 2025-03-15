package io.masterkun.stateeasy.core;

import java.util.Map;

public record Snapshot<STATE>(STATE state, long eventId, Map<String, String> metadata) {
}
