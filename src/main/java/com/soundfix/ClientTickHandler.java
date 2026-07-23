package com.soundfix;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public class ClientTickHandler {
    private ItemStack lastMainHandItem = ItemStack.EMPTY;
    private ItemStack lastOffHandItem = ItemStack.EMPTY;
    private Method stopMethod;
    private KeyMapping inspectKeyMapping;
    private long lastInspectPressTime = 0;
    private static final long INSPECT_TIMEOUT = 3000;

    public ClientTickHandler() {
        try {
            Class<?> clazz = Class.forName("com.tacz.guns.client.sound.SoundPlayManager");
            stopMethod = clazz.getDeclaredMethod("stopAndClearTrackedSounds");
            stopMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Class<?> keyClass = Class.forName("com.tacz.guns.client.input.InspectKey");
            Field field = keyClass.getDeclaredField("INSPECT_KEY");
            field.setAccessible(true);
            inspectKeyMapping = (KeyMapping) field.get(null);
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack currentMain = mc.player.getMainHandItem();
        ItemStack currentOff = mc.player.getOffhandItem();

        boolean mainChanged = !ItemStack.isSameItem(lastMainHandItem, currentMain);
        boolean offChanged = !ItemStack.isSameItem(lastOffHandItem, currentOff);

        if (mainChanged || offChanged) {
            lastMainHandItem = currentMain.copy();
            lastOffHandItem = currentOff.copy();
            stopSounds();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onKey(InputEvent.Key event) {
        if (inspectKeyMapping != null && event.getAction() == 1 && inspectKeyMapping.matches(event.getKey(), event.getScanCode())) {
            stopSounds();
            lastInspectPressTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onMouseButton(InputEvent.MouseButton.Pre event) {
        int btn = event.getButton();
        if (btn != 0 && btn != 1) return;
        if (event.getAction() != 1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        if (System.currentTimeMillis() - lastInspectPressTime > INSPECT_TIMEOUT) return;

        stopSounds();
    }

    private void stopSounds() {
        if (stopMethod != null) {
            try {
                stopMethod.invoke(null);
            } catch (Exception ignored) {}
        }
    }
}
