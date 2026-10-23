package com.bettercontent.betterrailbeetle.nativefixture;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ProducerAbiTest {
    static final String SECTION="net/minecraft/world/level/chunk/LevelChunkSection";
    static final String SECTION_DESC="(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;";
    static final String SET_DESC="(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;";
    ClassNode read(String name) throws Exception {
        try(var input=getClass().getClassLoader().getResourceAsStream(name+".class")) {
            assertNotNull(input,name); var node=new ClassNode();new ClassReader(input).accept(node,0);return node;
        }
    }
    List<MethodInsnNode> calls(MethodNode method) {
        var list=new ArrayList<MethodInsnNode>();for(var i:method.instructions)if(i instanceof MethodInsnNode call)list.add(call);return list;
    }
    Map<String,Object> values(AnnotationNode a) {
        var result=new HashMap<String,Object>();for(int i=0;i<a.values.size();i+=2)result.put((String)a.values.get(i),a.values.get(i+1));return result;
    }
    @Test void pinnedLevelProtoAndInheritedMapMatchExactTypedAbi() throws Exception {
        for(String type:List.of("LevelChunk","ProtoChunk")) {
            var node=read("net/minecraft/world/level/chunk/"+type);
            var method=node.methods.stream().filter(m->m.name.equals("setBlockState")&&m.desc.equals(SET_DESC)).findFirst().orElseThrow();
            assertEquals(1,calls(method).stream().filter(c->c.owner.equals(SECTION)&&c.name.equals("setBlockState")&&c.desc.equals(SECTION_DESC)).count());
        }
        var access=read("net/minecraft/world/level/chunk/ChunkAccess");
        var field=access.fields.stream().filter(f->f.name.equals("pendingBlockEntities")).findFirst().orElseThrow();
        assertEquals("Ljava/util/Map;",field.desc); assertTrue(field.signature.contains("BlockPos;Lnet/minecraft/nbt/CompoundTag;"));
        var api=read("com/llamalad7/mixinextras/injector/wrapmethod/WrapMethod");
        assertTrue(api.methods.stream().anyMatch(m->m.name.equals("require")&&m.desc.equals("()I")));
    }
    void redirectContract(MethodNode handler) {
        var calls=calls(handler);
        var nativeCalls=calls.stream().filter(c->c.owner.equals(SECTION)).toList();
        assertEquals(1,nativeCalls.size());assertEquals("setBlockState",nativeCalls.get(0).name);assertEquals(SECTION_DESC,nativeCalls.get(0).desc);
        assertSame(nativeCalls.get(0),calls.get(0)); // native forwarding FIRST, before observer access
        for(var i:handler.instructions) {
            assertFalse(i instanceof InvokeDynamicInsnNode,"No hot lambda");
            assertFalse(i instanceof TypeInsnNode t&&(t.getOpcode()==Opcodes.NEW||t.getOpcode()==Opcodes.ANEWARRAY),"No hot allocation");
            if(i instanceof MethodInsnNode c) assertFalse(c.name.equals("valueOf")||c.owner.equals("java/lang/reflect/Method"),"No hot boxing/reflection");
        }
    }
    @Test void compiledRedirectIsExactOnceFirstAllocationFreeAndNegativesReject() throws Exception {
        var node=read("com/bettercontent/betterrailbeetle/nativeclear/mixin/ProducerSectionMixin");
        var handler=node.methods.stream().filter(m->m.name.equals("observeActualSectionWrite")).findFirst().orElseThrow();
        redirectContract(handler);
        var annotation=handler.visibleAnnotations.stream().filter(a->a.desc.endsWith("/Redirect;")).findFirst().orElseThrow();var fields=values(annotation);
        for(String key:List.of("require","expect","allow"))assertEquals(1,fields.get(key));
        assertEquals(List.of("setBlockState"+SET_DESC),fields.get("method"));assertEquals(false,fields.get("remap"));
        assertEquals("L"+SECTION+";setBlockState"+SECTION_DESC,values((AnnotationNode)fields.get("at")).get("target"));
        handler.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,SECTION,"setBlockState",SECTION_DESC,false));
        assertThrows(AssertionError.class,()->redirectContract(handler));
        handler.instructions.remove(handler.instructions.getLast());handler.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/Object"));
        assertThrows(AssertionError.class,()->redirectContract(handler));
    }
    @Test void compiledScopesPinExactMethodsAndForwardOnceWithFinally() throws Exception {
        var nativeType=read("net/minecraft/gametest/framework/StructureUtils");
        var scopes=read("com/bettercontent/betterrailbeetle/nativeclear/mixin/ProducerScopeMixin");
        for(var method:scopes.methods)if(!method.name.equals("<init>")) {
            var annotation=method.visibleAnnotations.stream().filter(a->a.desc.endsWith("/WrapMethod;")).findFirst().orElseThrow();
            var fields=values(annotation);for(String key:List.of("require","expect","allow"))assertEquals(1,fields.get(key));
            String target=(String)((List<?>)fields.get("method")).get(0);
            assertTrue(nativeType.methods.stream().anyMatch(m->(m.name+m.desc).equals(target)));
            assertEquals(1,calls(method).stream().filter(c->c.owner.endsWith("/Operation")&&c.name.equals("call")).count());
            assertTrue(method.tryCatchBlocks.stream().anyMatch(b->b.type==null),"Original call must be balanced by finally");
        }
    }
    @Test void helperNamespaceStartupSelectionShutdownOutputAndArchiveExclusions() throws Exception {
        var prefixes=new ArrayList<String>();
        for(String path:List.of("src/main/resources/better_rail_beetle.mixins.json","src/nativeClear/resources/better_rail_beetle_native_clear.mixins.json","src/nativeFixture/resources/better_rail_beetle_native_witness.mixins.json")) {
            var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(path))).getAsJsonObject();
            prefixes.add(json.get("package").getAsString()+".");
        }
        for(String helper:List.of("ProducerCapture","ProducerLedger","ProducerPalette","ProducerLifecycle")) {
            String name="com.bettercontent.betterrailbeetle.nativeclear."+helper;
            assertTrue(prefixes.stream().noneMatch(name::startsWith),name);
        }
        var lifecycle=read("com/bettercontent/betterrailbeetle/nativeclear/ProducerLifecycle");
        assertTrue(lifecycle.visibleAnnotations.stream().anyMatch(a->a.desc.endsWith("Mod$EventBusSubscriber;")));
        for(var m:lifecycle.methods)for(var c:calls(m)) {
            if(c.owner.equals("org/slf4j/Logger")){assertEquals("stopped",m.name);assertEquals("info",c.name);assertTrue(m.desc.contains("ServerStoppedEvent"));}
            if(c.owner.startsWith("net/minecraft/")){
                assertTrue(m.name.equals("starting")||m.name.startsWith("lambda$starting$"),m.name);
                if(m.name.equals("starting"))assertTrue(m.desc.contains("ServerAboutToStartEvent"));
            }
            assertFalse(c.owner.startsWith("java/lang/reflect/")||c.owner.startsWith("java/nio/file/"));
        }
        var nativeServer=read("net/minecraft/gametest/framework/GameTestServer");
        var init=nativeServer.methods.stream().filter(m->m.name.equals("initServer")).findFirst().orElseThrow();
        var initCalls=calls(init);int about=-1,load=-1;
        for(int i=0;i<initCalls.size();i++){if(initCalls.get(i).name.equals("handleServerAboutToStart"))about=i;if(initCalls.get(i).name.equals("loadLevel"))load=i;}
        assertTrue(about>=0&&load>about,"Dictionary event must precede native level/chunk creation");
        var root=java.nio.file.Path.of(System.getProperty("bc.nativeWitness.testsRoot","build/native-witness-unit"),"producer-archive-"+UUID.randomUUID());java.nio.file.Files.createDirectories(root);
        int index=0;
        for(String entry:List.of("com/bettercontent/betterrailbeetle/nativeclear/ProducerCapture.class","com/bettercontent/betterrailbeetle/nativeclear/ProducerLifecycle.java","com/bettercontent/betterrailbeetle/nativeclear/mixin/ProducerPendingAccess.class")) {
            var path=root.resolve("negative-"+index+++".zip");
            try(var zip=new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(path))){zip.putNextEntry(new java.util.zip.ZipEntry(entry));zip.write(1);zip.closeEntry();}
            assertThrows(IllegalStateException.class,()->WitnessArchiveGuard.check(path));assertTrue(java.nio.file.Files.exists(path));
        }
    }
    @Test void actualCaptureAllocationsFollowBudgetAndSchemaNamesAreComplete() throws Exception {
        var capture=read("com/bettercontent/betterrailbeetle/nativeclear/ProducerCapture");
        var method=capture.methods.stream().filter(m->m.name.equals("transition")).findFirst().orElseThrow();
        int reserve=-1;for(int i=0;i<method.instructions.size();i++) {
            var insn=method.instructions.get(i);
            if(insn instanceof MethodInsnNode c&&c.name.equals("reserve"))reserve=i;
            if(insn instanceof InvokeDynamicInsnNode||insn instanceof TypeInsnNode t&&t.getOpcode()==Opcodes.NEW
                    ||insn instanceof MethodInsnNode c&&c.name.equals("valueOf"))assertTrue(reserve>=0&&i>reserve,"Allocation before candidate reservation");
        }
        assertTrue(reserve>=0);
        for(Class<?> record:List.of(com.bettercontent.betterrailbeetle.nativeclear.ProducerLedger.Scope.class,
                com.bettercontent.betterrailbeetle.nativeclear.ProducerLedger.Pending.class,
                com.bettercontent.betterrailbeetle.nativeclear.ProducerLedger.Entry.class,
                com.bettercontent.betterrailbeetle.nativeclear.ProducerLedger.Summary.class))
            for(var component:record.getRecordComponents())assertTrue(com.bettercontent.betterrailbeetle.nativeclear.ProducerPalette.SCHEMA.contains(component.getName()),component.getName());
        var source=java.nio.file.Files.readString(java.nio.file.Path.of("src/nativeClear/java/com/bettercontent/betterrailbeetle/nativeclear/ProducerLifecycle.java"));
        assertTrue(source.indexOf("block instanceof EntityBlock")<source.indexOf("getPossibleStates()"));
        assertTrue(source.indexOf("selected.sort")<source.indexOf("getPossibleStates()"));
        assertTrue(source.contains("getNamespace().equals(\"minecraft\")"));
    }
    void privacy(ClassNode node) {
        for(var field:node.fields)assertFalse(field.desc.contains("net/minecraft/"),field.name);
        for(var method:node.methods)for(var call:calls(method)) {
            assertFalse(call.owner.startsWith("java/io/")||call.owner.startsWith("java/nio/file/")||call.owner.startsWith("java/lang/reflect/")||call.owner.equals("org/slf4j/Logger"),call.owner);
            if(call.owner.startsWith("net/minecraft/")) assertTrue(
                    (call.owner.equals("net/minecraft/core/BlockPos")&&Set.of("getX","getY","getZ").contains(call.name))||
                    (call.owner.equals("net/minecraft/world/level/levelgen/structure/BoundingBox")&&Set.of("minX","minY","minZ","maxX","maxY","maxZ").contains(call.name))||
                    (call.owner.equals("net/minecraft/nbt/CompoundTag")&&Set.of("contains","getString","getInt","getBoolean","getAllKeys","getTagType").contains(call.name)),call.owner+"."+call.name);
        }
    }
    @Test void compiledCaptureIsFieldMapOnlyAndMetadataPrivacyNegativesReject() throws Exception {
        var node=read("com/bettercontent/betterrailbeetle/nativeclear/ProducerCapture");privacy(node);
        var iface=read("com/bettercontent/betterrailbeetle/nativeclear/mixin/ProducerPendingAccess");
        assertEquals(1,iface.methods.size());assertEquals("()Ljava/util/Map;",iface.methods.get(0).desc);
        var getter=iface.methods.get(0).visibleAnnotations.stream().filter(a->a.desc.endsWith("/Accessor;")).findFirst().orElseThrow();
        assertEquals("pendingBlockEntities",values(getter).get("value"));
        node.methods.get(0).instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"net/minecraft/world/level/Level","getBlockState","()V",false));
        assertThrows(AssertionError.class,()->privacy(node));
    }
}
