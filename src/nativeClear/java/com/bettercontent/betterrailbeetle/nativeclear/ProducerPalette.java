package com.bettercontent.betterrailbeetle.nativeclear;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Startup-built identity dictionary; lookups neither allocate nor inspect native state. */
public final class ProducerPalette {
    public static final int CAP = 131072;
    public static final String SCHEMA = "bc.rail_producer_witness.v1|selection=all_minecraft+all_EntityBlock_before_states|state=full_canonical_properties|scope=kind,sequence,parent,thread,depth,world_identity_hash,arg_identity_hash,scalars|transition=sequence,thread,ownerKind,x,y,z,oldState,requestedState,pending,activeForce,activeClear,lastCompletedForce,stack,stackTruncated|pending=id,x,y,z,keepPacked,keyTypes,keysTruncated|summary=candidates,budgetDropped,unknownStates,captureFailures,scopes,scopeOverflow,crossThreadScopes,balanceFailures,danglingDepth,unboundTransitions,observationComplete,entries|scope=kind,sequence,parent,thread,depth,worldIdentityHash,argumentIdentityHash,floor,x0,y0,z0,x1,y1,z1|max_records=8|max_frames=24|max_depth=16|max_scopes=256|max_keys=32|scope_identity=identityHash_not_object_equality|limits=direct_section_bypass,earlier_producer,unbound_threads,failed_original_without_return";
    public record State(int index, String canonical) {}
    private final Map<Object, State> states;
    private final List<String> selectedBlocks;
    private final String digest;
    private final boolean complete;
    public ProducerPalette(IdentityHashMap<Object, State> input, List<String> selected, boolean complete) {
        if (input.size() > CAP) throw new IllegalArgumentException("Palette cap exceeded");
        states = Collections.unmodifiableMap(new IdentityHashMap<>(input));
        selectedBlocks = List.copyOf(selected);
        this.complete = complete;
        var lines = new java.util.ArrayList<String>();
        lines.add(SCHEMA);
        lines.add("complete="+complete);
        selectedBlocks.forEach(s -> lines.add("block=" + s));
        input.values().stream().sorted(java.util.Comparator.comparingInt(State::index))
                .forEach(s -> lines.add("state=" + s.index() + ":" + s.canonical()));
        digest = sha256(String.join("\n", lines));
    }
    public State lookup(Object identity) { return states.get(identity); }
    public int size() { return states.size(); }
    public boolean complete() { return complete; }
    public String digest() { return digest; }
    public String schemaDigest() { return sha256(SCHEMA); }
    public int selectedCount() { return selectedBlocks.size(); }
    public static String sha256(String text) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
