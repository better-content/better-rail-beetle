package com.bettercontent.betterrailbeetle.nativefixture;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Runs ACTUAL compiled handlers in a child realm with inert declared ABI stubs, not Minecraft. */
class ProducerForwardingTest {
    static final String P="com.bettercontent.betterrailbeetle.nativeclear.";
    @Test void actualCompiledHandlersPreserveReceiverPrimitivesResultThrowableAndScopeBalance() throws Exception {
        var root=Path.of(System.getProperty("bc.nativeWitness.testsRoot","build/native-witness-unit"),"producer-forward-"+UUID.randomUUID());
        Files.createDirectories(root);
        var sources=new LinkedHashMap<String,String>();
        sources.put("net.minecraft.core.BlockPos","public class BlockPos {}");
        sources.put("net.minecraft.server.level.ServerLevel","public class ServerLevel {}");
        sources.put("net.minecraft.world.level.levelgen.structure.BoundingBox","public class BoundingBox {}");
        sources.put("net.minecraft.world.level.block.state.BlockState","public class BlockState {}");
        sources.put("net.minecraft.world.level.chunk.ProtoChunk","public class ProtoChunk {}");
        sources.put("net.minecraft.world.level.chunk.LevelChunkSection","public class LevelChunkSection { public int calls,x,y,z; public net.minecraft.world.level.block.state.BlockState requested,old; public Throwable failure; public net.minecraft.world.level.block.state.BlockState setBlockState(int a,int b,int c,net.minecraft.world.level.block.state.BlockState state){calls++;x=a;y=b;z=c;requested=state;if(failure instanceof RuntimeException r)throw r;if(failure instanceof Error e)throw e;return old;} }");
        sources.put(P+"mixin.ProducerPendingAccess","public interface ProducerPendingAccess { java.util.Map railProducerPending(); }");
        sources.put(P+"ProducerCapture","public class ProducerCapture { public static boolean fault; public static int observed,failed,depth,enters,exits; public static Object oldSeen,newSeen,posSeen,scopeArg,worldSeen; public static int floorSeen; public static void transition("+P+"mixin.ProducerPendingAccess o,net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState a,net.minecraft.world.level.block.state.BlockState b,boolean proto){observed++;oldSeen=a;newSeen=b;posSeen=p;if(fault)throw new AssertionError(\"observer failure\");} public static void failed(){failed++;} public static long enterForce(net.minecraft.core.BlockPos p,net.minecraft.server.level.ServerLevel w){enters++;depth++;scopeArg=p;worldSeen=w;return enters;} public static long enterClear(net.minecraft.world.level.levelgen.structure.BoundingBox b,int f,net.minecraft.server.level.ServerLevel w){enters++;depth++;scopeArg=b;floorSeen=f;worldSeen=w;return enters;} public static void exit(long ticket){depth--;exits++;} }");
        var compiler=ToolProvider.getSystemJavaCompiler();assertNotNull(compiler,"JDK compiler required for declared inert forwarding controls");
        var files=new ArrayList<JavaFileObject>();
        for(var entry:sources.entrySet()) {
            String name=entry.getKey(), body=entry.getValue(), pkg=name.substring(0,name.lastIndexOf('.'));
            files.add(new SimpleJavaFileObject(URI.create("string:///"+name.replace('.','/')+".java"),JavaFileObject.Kind.SOURCE){
                @Override public CharSequence getCharContent(boolean ignore){return "package "+pkg+";"+body;}
            });
        }
        try(var manager=compiler.getStandardFileManager(null,null,null)) {
            assertTrue(compiler.getTask(null,manager,null,List.of("-proc:none","--release","17","-d",root.toString()),null,files).call());
        }
        // Generate concrete instance without modifying the actual handler's bytes.
        var cw=new org.objectweb.asm.ClassWriter(0);
        String concrete=P.replace('.','/')+"mixin/ProducerConcrete";
        cw.visit(61,org.objectweb.asm.Opcodes.ACC_PUBLIC,concrete,null,(P+"mixin.ProducerSectionMixin").replace('.','/'),new String[]{(P+"mixin.ProducerPendingAccess").replace('.','/')});
        var init=cw.visitMethod(1,"<init>","()V",null,null);init.visitCode();init.visitVarInsn(25,0);init.visitMethodInsn(183,(P+"mixin.ProducerSectionMixin").replace('.','/'),"<init>","()V",false);init.visitInsn(177);init.visitMaxs(1,1);init.visitEnd();
        var getter=cw.visitMethod(1,"railProducerPending","()Ljava/util/Map;",null,null);getter.visitCode();getter.visitMethodInsn(184,"java/util/Collections","emptyMap","()Ljava/util/Map;",false);getter.visitInsn(176);getter.visitMaxs(1,1);getter.visitEnd();cw.visitEnd();
        var bytes=new HashMap<String,byte[]>();bytes.put(concrete.replace('/','.'),cw.toByteArray());
        for(String hook:List.of("ProducerSectionMixin","ProducerScopeMixin"))try(var input=getClass().getClassLoader().getResourceAsStream((P+"mixin."+hook).replace('.','/')+".class")){assertNotNull(input);bytes.put(P+"mixin."+hook,input.readAllBytes());}
        try(var loader=new URLClassLoader(new URL[]{root.toUri().toURL()},getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                synchronized(getClassLoadingLock(name)) {
                    Class<?> type=findLoadedClass(name);
                    if(type==null&&bytes.containsKey(name)){byte[] code=bytes.get(name);type=defineClass(name,code,0,code.length);}
                    if(type==null&&sources.containsKey(name))type=findClass(name);
                    if(type==null)type=super.loadClass(name,false);
                    if(resolve)resolveClass(type);return type;
                }
            }
        }) {
            var sectionType=loader.loadClass("net.minecraft.world.level.chunk.LevelChunkSection");Object section=sectionType.getConstructor().newInstance();
            var stateType=loader.loadClass("net.minecraft.world.level.block.state.BlockState");Object old=stateType.getConstructor().newInstance(), requested=stateType.getConstructor().newInstance();
            Object position=loader.loadClass("net.minecraft.core.BlockPos").getConstructor().newInstance();
            var observer=loader.loadClass(P+"ProducerCapture");var hook=loader.loadClass(P+"mixin.ProducerSectionMixin");
            var handler=Arrays.stream(hook.getDeclaredMethods()).filter(m->m.getName().equals("observeActualSectionWrite")).findFirst().orElseThrow();handler.setAccessible(true);
            Object instance=loader.loadClass(concrete.replace('/','.')).getConstructor().newInstance();sectionType.getField("old").set(section,old);
            for(boolean fault:new boolean[]{false,true}) {
                observer.getField("fault").set(null,fault);
                assertSame(old,handler.invoke(instance,section,3,4,5,requested,position,requested,false));
                assertSame(requested,sectionType.getField("requested").get(section));assertEquals(3,sectionType.getField("x").getInt(section));assertEquals(4,sectionType.getField("y").getInt(section));assertEquals(5,sectionType.getField("z").getInt(section));
                assertSame(old,observer.getField("oldSeen").get(null));assertSame(requested,observer.getField("newSeen").get(null));assertSame(position,observer.getField("posSeen").get(null));
            }
            for(Throwable originalFailure:new Throwable[]{new IllegalStateException("original section failure"),new AssertionError("original section error")}) {
                sectionType.getField("failure").set(section,originalFailure);
                assertSame(originalFailure,assertThrows(InvocationTargetException.class,()->handler.invoke(instance,section,3,4,5,requested,position,requested,false)).getCause());
            }
            assertEquals(4,sectionType.getField("calls").getInt(section));assertEquals(2,observer.getField("observed").getInt(null));assertEquals(1,observer.getField("failed").getInt(null));
            var scopes=loader.loadClass(P+"mixin.ProducerScopeMixin");Object world=loader.loadClass("net.minecraft.server.level.ServerLevel").getConstructor().newInstance();Object bounds=loader.loadClass("net.minecraft.world.level.levelgen.structure.BoundingBox").getConstructor().newInstance();
            for(String methodName:List.of("forceScope","clearScope")) {
                var method=Arrays.stream(scopes.getDeclaredMethods()).filter(m->m.getName().equals(methodName)).findFirst().orElseThrow();method.setAccessible(true);
                for(boolean fail:new boolean[]{false,true}) {
                    int[] calls={0};var failure=new IllegalArgumentException("original scope failure");
                    Operation<Void> original=args->{calls[0]++;assertSame(methodName.equals("forceScope")?position:bounds,args[0]);assertSame(world,args[args.length-1]);if(methodName.equals("clearScope"))assertEquals(-60,args[1]);if(fail)throw failure;return null;};
                    Object[] args=methodName.equals("forceScope")?new Object[]{position,world,original}:new Object[]{bounds,-60,world,original};
                    if(fail)assertSame(failure,assertThrows(InvocationTargetException.class,()->method.invoke(null,args)).getCause());else method.invoke(null,args);
                    assertEquals(1,calls[0]);assertEquals(0,observer.getField("depth").getInt(null));
                }
            }
            assertEquals(4,observer.getField("enters").getInt(null));assertEquals(4,observer.getField("exits").getInt(null));
        }
    }
}
