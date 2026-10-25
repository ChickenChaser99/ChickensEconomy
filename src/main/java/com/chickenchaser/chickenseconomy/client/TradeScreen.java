package com.chickenchaser.chickenseconomy.client;

import com.chickenchaser.chickenseconomy.menu.TradeMenu;
import com.chickenchaser.chickenseconomy.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private EditBox moneyInput;
    private Button readyBtn;

    public TradeScreen(TradeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
        this.inventoryLabelY = 88; // Lower inventory label to clear the trade zone
    }

    @Override
    protected void init() {
        super.init();
        int left = this.leftPos;
        int top = this.topPos;

        // Money offer text field - placed cleanly beneath your trade grid
        this.moneyInput = new EditBox(this.font, left + 24, top + 64, 42, 14, Component.literal("0"));
        this.moneyInput.setValue("0");
        this.moneyInput.setMaxLength(8);
        this.moneyInput.setResponder(text -> {
            try {
                long val = text.isEmpty() ? 0 : Long.parseLong(text.replaceAll("[^0-9]", ""));
                ModNetworking.CHANNEL.sendToServer(new ModNetworking.UpdateMoneyOfferC2SPacket(val));
            } catch (NumberFormatException ignored) {}
        });
        addWidget(this.moneyInput);

        // Ready / Lock Button - anchored on the bottom-right of the trade zone
        this.readyBtn = addRenderableWidget(Button.builder(Component.literal("Lock In"), btn -> {
            ModNetworking.CHANNEL.sendToServer(new ModNetworking.ToggleReadyC2SPacket());
        }).bounds(left + 104, top + 63, 56, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        // Main dark canvas
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xEE1E1E24);
        // Outer border
        graphics.renderOutline(left, top, this.imageWidth, this.imageHeight, 0xFF3C3F41);

        // Header separator
        graphics.fill(left + 6, top + 15, left + this.imageWidth - 6, top + 16, 0x44FFFFFF);

        // Left Panel (Your Side)
        graphics.fill(left + 10, top + 18, left + 82, top + 83, 0x44000000);
        graphics.renderOutline(left + 10, top + 18, 72, 65, 0x55AAAAAA);

        // Right Panel (Partner Side)
        graphics.fill(left + 94, top + 18, left + 166, top + 83, 0x44000000);
        graphics.renderOutline(left + 94, top + 18, 72, 65, 0x55AAAAAA);

        // Slot backgrounds for Left 2x2
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                drawSlotBox(graphics, left + 25 + c * 18, top + 22 + r * 18);
            }
        }

        // Slot backgrounds for Right 2x2
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                drawSlotBox(graphics, left + 119 + c * 18, top + 22 + r * 18);
            }
        }

        // Slot backgrounds for Player Inventory & Hotbar
        int invY = top + 98;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                drawSlotBox(graphics, left + 8 + c * 18, invY + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            drawSlotBox(graphics, left + 8 + c * 18, invY + 58);
        }
    }

    private void drawSlotBox(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        graphics.fill(x, y, x + 16, y + 16, 0xFF141414);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.moneyInput.render(graphics, mouseX, mouseY, partialTick);

        int left = this.leftPos;
        int top = this.topPos;

        // Panel Titles
        graphics.drawString(this.font, "Your Offer", left + 12, top + 6, 0x55FF55, false);
        graphics.drawString(this.font, "Their Offer", left + 96, top + 6, 0xAAAAAA, false);

        // Cash labels
        graphics.drawString(this.font, "$", left + 15, top + 67, 0xFFD700, false);
        long theirMoney = menu.isHost() ? menu.getGuestMoney() : menu.getHostMoney();
        graphics.drawString(this.font, "$" + theirMoney, left + 112, top + 46, 0xFFD700, true);

        // Status indicators
        boolean myReady = menu.isHost() ? menu.isHostReady() : menu.isGuestReady();
        boolean otherReady = menu.isHost() ? menu.isGuestReady() : menu.isHostReady();

        this.readyBtn.setMessage(Component.literal(myReady ? "Locked" : "Lock In"));

        // Ready pill tags
        String otherStatus = otherReady ? "READY" : "WAITING";
        int otherColor = otherReady ? 0x55FF55 : 0xFF5555;
        graphics.drawCenteredString(this.font, "[" + otherStatus + "]", left + 130, top + 66, otherColor);

        renderTooltip(graphics, mouseX, mouseY);
    }
}