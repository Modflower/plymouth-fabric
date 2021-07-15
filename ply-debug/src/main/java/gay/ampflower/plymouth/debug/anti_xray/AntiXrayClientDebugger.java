package gay.ampflower.plymouth.debug.anti_xray;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import gay.ampflower.plymouth.antixray.Constants;
import gay.ampflower.plymouth.debug.DebugProfiler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;

import java.util.BitSet;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;
import static gay.ampflower.plymouth.debug.Fusebox.*;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public class AntiXrayClientDebugger {
    // Positions to render. By default, all will render at 0, 0, 0.
    public static final DebugProfiler
            antiXraySet = new DebugProfiler(2048, 0, 1, 0, 0.095F),
            antiXrayUpdate = new DebugProfiler(64, 1, 0, 0, 0.1F),
            antiXrayTest = new DebugProfiler(2048, 0, 0, 1, 0.050F),
            onBlockDelta = new DebugProfiler(128, 1, 1, 0, 0.075F),
            onBlockEvent = new DebugProfiler(128, 0, 1, 0, 0.090F),
            onBlockEntityUpdate = new DebugProfiler(128, 1, 0.5F, 0, 0.110F),
            onChunkLoad = new DebugProfiler(128, 0, .75F, 0.15F, 0.050F, 16),
            onChunkBlockEntity = new DebugProfiler(128, 1, 0, 0.5F, 0.115F);
    public static int mx, mz;
    public static BitSet[] masks;

    public static void initialise() {
        ClientPlayNetworking.registerGlobalReceiver(AntiXrayDebugger.debugAntiXraySet, AntiXrayClientDebugger::handleAntiXraySet);
        ClientPlayNetworking.registerGlobalReceiver(AntiXrayDebugger.debugAntiXrayUpdate, AntiXrayClientDebugger::handleAntiXrayUpdate);
        ClientPlayNetworking.registerGlobalReceiver(AntiXrayDebugger.debugAntiXrayTest, AntiXrayClientDebugger::handleAntiXrayTest);
        ClientPlayNetworking.registerGlobalReceiver(AntiXrayDebugger.debugAntiXrayMask, AntiXrayClientDebugger::handleAntiXrayMask);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(AntiXrayClientDebugger::render);
        ClientCommandManager.DISPATCHER.register(literal("pdb").then(literal("ax").then(literal("clear").executes(ctx -> {
            masks = null;
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
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();

        var tessellator = Tessellator.getInstance();
        var immediate = tessellator.getBuffer();

        // TODO: figure out ctx.matrixStack()
        var matrices = new MatrixStack();
        var camera = ctx.camera().getPos();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        RenderSystem.disableTexture();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(0.5F);

        immediate.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        if (VIEW_AX_SET) antiXraySet.render(matrices, immediate);
        if (VIEW_AX_UPDATE) antiXrayUpdate.render(matrices, immediate);
        if (VIEW_AX_TEST) antiXrayTest.render(matrices, immediate);
        if (VIEW_BLOCK_DELTA) onBlockDelta.render(matrices, immediate);
        if (VIEW_BLOCK_EVENT) onBlockEvent.render(matrices, immediate);
        if (VIEW_BLOCK_ENTITY_UPDATE) onBlockEntityUpdate.render(matrices, immediate);
        if (VIEW_CHUNK_LOAD) onChunkLoad.render(matrices, immediate);
        if (VIEW_CHUNK_BLOCK_ENTITY) onChunkBlockEntity.render(matrices, immediate);
        renderMask(matrices, immediate, ctx.world().getBottomY());

        tessellator.draw();

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
    }

    private static void renderMask(MatrixStack stack, VertexConsumer consumer, int yoff) {
        if (masks == null) return;

        for (int w = 0; w < masks.length; w++) {
            var mask = masks[w];
            if (mask == null) continue;
            stack.push();
            stack.translate(mx * 16, w * 16 + yoff, mz * 16);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (mask.get(Constants.toIndex(x, y, z)))
                            WorldRenderer.drawBox(stack, consumer, x, y, z, x + 1, y + 1, z + 1, .5F, .5F, .5F, .5F);
                    }
                }
            }
            stack.pop();
        }
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
        int cx = buf.readVarInt(), cz = buf.readVarInt(), ml = buf.readVarInt();
        var ms = new BitSet[ml];
        for (int i = 0; i < ml; i++) {
            var arr = buf.readLongArray(null, 64);
            if (arr.length != 0) {
                ms[i] = BitSet.valueOf(arr);
            }
        }
        client.execute(() -> {
            mx = cx;
            mz = cz;
            masks = ms;
        });
    }
}
