package com.chickenchaser.chickenseconomy.client;

import com.chickenchaser.chickenseconomy.Main;
import com.chickenchaser.chickenseconomy.client.ClientShopData.ClientShopEntry;
import com.chickenchaser.chickenseconomy.shop.ShopSavedData.ShopMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShopHologramRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        for (var entry : ClientShopData.getAll().entrySet()) {
            BlockPos pos = entry.getKey();
            ClientShopEntry shop = entry.getValue();

            // Render limit: within 16 blocks (16^2 = 256)
            if (pos.distToCenterSqr(cameraPos) > 256.0D) {
                continue;
            }

            ItemStack displayStack = shop.displayStack();

            double x = pos.getX() + 0.5D - cameraPos.x;
            double y = pos.getY() + 1.25D - cameraPos.y;
            double z = pos.getZ() + 0.5D - cameraPos.z;

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            // 1. Render Floating Spinning Item
            if (displayStack != null && !displayStack.isEmpty()) {
                poseStack.pushPose();
                float rotation = (level.getGameTime() + event.getPartialTick()) * 3.0F;
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
                poseStack.scale(1.0F, 1.0F, 1.0F);

                ItemRenderer itemRenderer = mc.getItemRenderer();
                itemRenderer.renderStatic(displayStack, ItemDisplayContext.GROUND,
                        0xF000F0, 0, poseStack, bufferSource, level, 0);
                poseStack.popPose();
            }

            // 2. Render Billboarded Text Above Item
            poseStack.translate(0.0D, 0.55D, 0.0D);
            poseStack.mulPose(event.getCamera().rotation());
            poseStack.scale(-0.025F, -0.025F, 0.025F);

            Font font = mc.font;
            Matrix4f matrix = poseStack.last().pose();

            // Line 1: [BUYING] or [SELLING] Tag
            String prefix = shop.isServer() ? "SERVER " : "";
            boolean isBuy = shop.mode() == ShopMode.BUY;
            Component tag = Component.literal("[" + prefix + (isBuy ? "BUY" : "SELL") + "]");
            int tagColor = shop.isServer() ? 0xDD55FF : (isBuy ? 0x55FF55 : 0xFFAA00);
            font.drawInBatch(tag, -font.width(tag) / 2.0F, -10, tagColor, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);

            // Line 2: Quantity & Price
            Component details = Component.literal(shop.quantity() + "x for $" + shop.price());
            font.drawInBatch(details, -font.width(details) / 2.0F, 2, 0xFFFFFF, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);

            poseStack.popPose();

        }

        bufferSource.endBatch();
    }
}