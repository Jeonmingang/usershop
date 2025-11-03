package com.minkang.wild;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShoutManager {
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<UUID, String> lastMsg = new HashMap<>();

    public long remaining(UUID id, int cdSeconds) {
        if (cdSeconds <= 0) return 0L;
        long now = System.currentTimeMillis();
        long next = lastUse.getOrDefault(id, 0L) + (cdSeconds * 1000L);
        return Math.max(0L, next - now);
    }
    public void stamp(UUID id) { lastUse.put(id, System.currentTimeMillis()); }
    public String getLastMsg(UUID id) { return lastMsg.get(id); }
    public void setLastMsg(UUID id, String m) { lastMsg.put(id, m); }
}
