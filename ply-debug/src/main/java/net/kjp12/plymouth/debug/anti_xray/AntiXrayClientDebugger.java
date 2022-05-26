package net.kjp12.plymouth.debug.anti_xray;// Created 2021-04-04T10:59:04

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.kjp12.plymouth.debug.DebugProfiler;
import net.kjp12.plymouth.debug.Fusebox;
import net.kjp12.plymouth.debug.RenderBatch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;

import java.util.BitSet;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.kjp12.plymouth.debug.Fusebox.*;

/**
 * @author KJP12
 * @since 0.0.0
 */
public class AntiXrayClientDebugger {
    // Positions to render. By default, all will render at 0, 0, 0.
    public static final DebugProfiler
            antiXraySet = new DebugProfiler(Fusebox.viewAntiXraySetLimit, 0, 255, 0, 0.095F),
            antiXrayUpdate = new DebugProfiler(Fusebox.viewAntiXrayUpdateLimit, 255, 0, 0, 0.1F),
            antiXrayTest = new DebugProfiler(Fusebox.viewAntiXrayTestLimit, 0, 0, 255, 0.050F),
            onBlockDelta = new DebugProfiler(Fusebox.viewBlockDeltaLimit, 255, 255, 0, 0.075F),
            onBlockEvent = new DebugProfiler(Fusebox.viewBlockEventLimit, 0, 255, 0, 0.090F),
            onBlockEntityUpdate = new DebugProfiler(Fusebox.viewBlockEntityUpdateLimit, 255, 128, 0, 0.110F),
            onChunkLoad = new DebugProfiler(Fusebox.viewChunkLoadLimit, 0, 192, 38, 0.050F, 16),
            onChunkBlockEntity = new DebugProfiler(Fusebox.viewChunkBlockEntityLimit, 255, 0, 128, 0.115F);
    public static int mx, mz;
    public static BitSet mask;

    public static void initialise() {
        ClientPlayNetworking.registerGlobalReceiver(AntiXrayDebugger.debugAntiXraySet, AntiXrayClientDebugger::handleAntiXraySet);
        ClientPlayNetworking.registerGlobalReceiver(AntiXrayDebugger.debugAntiXrayUpdate, AntiXrayClientDebugger::handleAntiXrayUpdate);
        ClientPlayNetworking.registerGlobalReceiver(AntiXrayDebugger.debugAntiXrayTest, AntiXrayClientDebugger::handleAntiXrayTest);
        ClientPlayNetworking.registerGlobalReceiver(AntiXrayDebugger.debugAntiXrayMask, AntiXrayClientDebugger::handleAntiXrayMask);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(AntiXrayClientDebugger::render);
        ClientCommandManager.DISPATCHER.register(literal("pdb").then(literal("ax").then(literal("clear").executes(ctx -> {
            mask = null;
            antiXraySet.clear();
            antiXrayUpdate.clear();
            antiXrayTest.clear();
            onBlockDelta.clear();
            onBlockEvent.clear();
            onBlockEntityUpdate.clear();
            onChunkLoad.clear();
            onChunkBlockEntity.clear();
            return 1;
        }))));
    }

    private static void render(WorldRenderContext ctx) {
        // Note: We purposely don't enable a depth test for the sake of visibility.
        // However, due to which stage it's on, it does get obstructed by water and clouds.
        RenderBatch.beginBatch();

        // TODO: figure out ctx.matrixStack()
        var matrices = new MatrixStack();
        var camera = ctx.camera().getPos();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (viewAntiXraySet) antiXraySet.render(matrices);
        if (viewAntiXrayUpdate) antiXrayUpdate.render(matrices);
        if (viewAntiXrayTest) antiXrayTest.render(matrices);
        if (viewBlockDelta) onBlockDelta.render(matrices);
        if (viewBlockEvent) onBlockEvent.render(matrices);
        if (viewBlockEntityUpdate) onBlockEntityUpdate.render(matrices);
        if (viewChunkLoad) onChunkLoad.render(matrices);
        if (viewChunkBlockEntity) onChunkBlockEntity.render(matrices);
        renderMask(matrices, ctx.world().getBottomY());

        RenderBatch.endBatch();
    }

    private static void renderMask(MatrixStack stack, int yoff) {
        if (mask == null) return;

        stack.push();
        stack.translate(mx * 16, yoff, mz * 16);
        /*
        var itr = mask.intIterator();
        while(itr.hasNext()) {
            int i = itr.nextInt();
        /**/
        int i = -1;
        while ((i = mask.nextSetBit(++i)) >= 0) {
            int x = i & 15;
            int z = (i >> 4) & 15;
            int y = (i >> 8);
            RenderBatch.drawSolidBox(stack.peek(), x, y, z, x + 1, y + 1, z + 1, 127, 127, 127, 127, false, false, false, false);
        }
        stack.pop();
    }

    private static void handleAntiXraySet(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        long pos = buf.readLong();
        client.execute(() -> antiXraySet.push(pos));
    }

    private static void handleAntiXrayUpdate(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        long pos = buf.readLong();
        client.execute(() -> antiXrayUpdate.push(pos));
    }

    private static void handleAntiXrayTest(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        long pos = buf.readLong();
        client.execute(() -> antiXrayTest.push(pos));
    }

    private static void handleAntiXrayMask(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        int cx = buf.readVarInt(), cz = buf.readVarInt();
        var nb = buf.readLongArray();
        var ms = BitSet.valueOf(nb);
        client.execute(() -> {
            mx = cx;
            mz = cz;
            mask = ms;
        });
    }

    public static int toIndex(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }
}
