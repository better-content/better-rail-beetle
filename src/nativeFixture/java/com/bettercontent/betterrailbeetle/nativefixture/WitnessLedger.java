package com.bettercontent.betterrailbeetle.nativefixture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Nonshipping, bounded scalar ledger. No Minecraft objects or arbitrary payloads are retained. */
public final class WitnessLedger {
    public static final int MAX_RECORDS = 1024;
    public static final int MAX_CHUNKS_PER_BOUNDARY = 64;
    public static final int MAX_BOUNDARIES = 128;
    public static final int MAX_EXISTING_POSITIONS = 4096;
    public static final int MAX_BYTES = 1024 * 1024;
    public static final int MAX_CALLSITES = 8;
    private final boolean enabled;
    private final List<JsonObject> records = new ArrayList<>();
    private long observed, overflow, missingChunks, boundaryOverflow, fieldTruncations, limitedCallsiteSlices;
    private int boundaries, existingPositions;
    private long unexaminedPositionBoundaries, incompleteScopes;
    private boolean sealed;

    public record Callsite(String className, String method, int line) {}
    public record Pending(String id, Integer x, Integer y, Integer z, Boolean keepPacked) {}
    public record Slice(List<Callsite> frames, boolean limited) {
        public Slice { frames = List.copyOf(frames); }
    }

    public WitnessLedger(boolean enabled) { this.enabled = enabled; }
    public boolean enabled() { return enabled; }

    public synchronized int beginBoundary() {
        if (!enabled) return -1;
        if (boundaries == MAX_BOUNDARIES || sealed) { boundaryOverflow++; return -1; }
        return ++boundaries;
    }
    public synchronized boolean examinePosition() {
        if (!enabled) return false;
        if (existingPositions == MAX_EXISTING_POSITIONS) { unexaminedPositionBoundaries++; return false; }
        existingPositions++; return true;
    }
    public synchronized boolean full() { return sealed || records.size() == MAX_RECORDS; }
    public synchronized void droppedRecord() { if (enabled) { observed++; overflow++; } }
    public synchronized void missingChunk() { if (enabled) missingChunks++; }
    public synchronized void incompleteScope() { if (enabled) incompleteScopes++; }
    public synchronized void chunkOverflow(long count) { if (enabled) boundaryOverflow += count; }

    public void record(String phase, int boundary, Pending pending, String blockState,
                       String chunkStatus, Slice callers) {
        record(phase, boundary, pending, blockState, null, chunkStatus, callers);
    }
    public synchronized void record(String phase, int boundary, Pending pending, String blockState,
                                    String proposedState, String chunkStatus, Slice callers) {
        if (!enabled) return;
        observed++;
        if (sealed || records.size() == MAX_RECORDS) { overflow++; return; }
        var row = new JsonObject();
        row.addProperty("phase", clipped(phase, 32));
        row.addProperty("boundary", boundary);
        // Only registry identifiers / the SDK's exact placeholder ID are permitted here.
        String id = pending.id();
        if (id.length() > 128 || !id.matches("DUMMY|[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            id = "invalid-native-id"; fieldTruncations++;
        }
        row.addProperty("id", clipped(id, 128));
        row.addProperty("x", pending.x()); row.addProperty("y", pending.y()); row.addProperty("z", pending.z());
        boolean coordinatesPresent = pending.x() != null && pending.y() != null && pending.z() != null;
        row.addProperty("nbt_coordinates_present", coordinatesPresent);
        if (!coordinatesPresent) fieldTruncations++;
        if (pending.keepPacked() != null) row.addProperty("keepPacked", pending.keepPacked());
        row.addProperty("block_state", clipped(blockState, 512));
        if (proposedState != null) row.addProperty("proposed_block_state", clipped(proposedState, 512));
        row.addProperty("chunk_status", clipped(chunkStatus, 64));
        var stack = new JsonArray();
        for (var frame : callers.frames().stream().limit(MAX_CALLSITES).toList()) {
            var call = new JsonObject();
            call.addProperty("class", clipped(frame.className(), 128));
            call.addProperty("method", clipped(frame.method(), 96));
            call.addProperty("line", frame.line()); stack.add(call);
        }
        row.add("callsites", stack);
        boolean callerLimited = callers.limited() || callers.frames().size() > MAX_CALLSITES;
        row.addProperty("callsite_slice_limited", callerLimited);
        if (callerLimited) limitedCallsiteSlices++;
        records.add(row);
    }

    private String clipped(String value, int limit) {
        if (value.length() <= limit) return value;
        fieldTruncations++; return value.substring(0, limit);
    }

    /** Serialization happens once after the run, never on the world mutation / promotion path. */
    public synchronized byte[] finish(String token) {
        if (!enabled) return new byte[0];
        if (sealed) throw new IllegalStateException("Native witness already sealed");
        sealed = true;
        var gson = new com.google.gson.GsonBuilder().serializeNulls().create();
        int low = 0, high = records.size();
        // Find a bounded prefix; byte overflow is explicit, never an invented complete observation.
        while (low < high) {
            int count = (low + high + 1) / 2;
            if (bytes(gson, token, count).length <= MAX_BYTES) low = count; else high = count - 1;
        }
        byte[] result = bytes(gson, token, low);
        if (result.length > MAX_BYTES) throw new IllegalStateException("Native witness envelope exceeds byte bound");
        return result;
    }
    private byte[] bytes(Gson gson, String token, int count) {
        var root = new JsonObject();
        root.addProperty("schema", "bc.rail_native_witness.v1");
        root.addProperty("run_token", token);
        root.addProperty("completion_event", "ServerStoppedEvent");
        root.addProperty("enabled", true);
        root.addProperty("max_records", MAX_RECORDS); root.addProperty("max_bytes", MAX_BYTES);
        root.addProperty("max_chunks_per_boundary", MAX_CHUNKS_PER_BOUNDARY);
        root.addProperty("max_boundaries", MAX_BOUNDARIES); root.addProperty("max_callsites", MAX_CALLSITES);
        root.addProperty("max_existing_positions", MAX_EXISTING_POSITIONS);
        root.addProperty("examined_existing_positions", existingPositions);
        root.addProperty("candidate_scope", "existing clear-flow old-BE-state mutation calls; repeated candidates consume budget; not a voxel or pending-map absence scan");
        root.addProperty("promotion_scope", "original state-read returns within the exact synchronous expanded clear scope only");
        root.addProperty("aborted_or_incomplete_scopes", incompleteScopes);
        root.addProperty("position_scan_truncated_boundaries", unexaminedPositionBoundaries);
        root.addProperty("boundaries", boundaries); root.addProperty("observed_records", observed);
        root.addProperty("retained_records", count); root.addProperty("overflow_records", overflow);
        root.addProperty("byte_dropped_records", records.size() - count);
        root.addProperty("missing_loaded_chunks", missingChunks);
        root.addProperty("boundary_or_chunk_overflow", boundaryOverflow);
        root.addProperty("field_truncations_or_redactions", fieldTruncations);
        boolean complete = overflow == 0 && count == records.size() && missingChunks == 0
                && boundaryOverflow == 0 && fieldTruncations == 0 && unexaminedPositionBoundaries == 0 && incompleteScopes == 0;
        root.addProperty("observation_scope_complete", complete);
        root.addProperty("callsite_slices_limited", limitedCallsiteSlices);
        root.addProperty("truncated_or_missing", !complete || limitedCallsiteSlices > 0);
        root.addProperty("callsite_scope", "finite prefix only; each record states whether its caller tail was omitted");
        var rows = new JsonArray(); for (int i = 0; i < count; i++) rows.add(records.get(i));
        root.add("records", rows);
        return (gson.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    }
}
