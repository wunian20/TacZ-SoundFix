package com.soundfix;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public class ClientTickHandler {
    private ItemStack lastMainHandItem = ItemStack.EMPTY;
    private ItemStack lastOffHandItem = ItemStack.EMPTY;
    private Class<?> soundManagerClass;
    private KeyMapping inspectKeyMapping;
    private long lastInspectPressTime = 0;
    /** 捕获到的当前检视音效实例引用（来自 SoundPlayManager.tmpSoundInstance） */
    private Object currentInspectSound = null;
    /** 按下检视键后，下一个客户端 tick 捕获检视音效 */
    private boolean captureInspectPending = false;
    private static final long INSPECT_TIMEOUT = 8000;

    public ClientTickHandler() {
        try {
            soundManagerClass = Class.forName("com.tacz.guns.client.sound.SoundPlayManager");
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
    public void onClientTick(ClientTickEvent.Post event) {
        // 捕获检视音效：检视键按下后 TaCZ 已在本 tick 之前播放检视音效并写入 tmpSoundInstance
        if (captureInspectPending) {
            captureInspectPending = false;
            currentInspectSound = readTmpSoundInstance();
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack currentMain = mc.player.getMainHandItem();
        ItemStack currentOff = mc.player.getOffhandItem();

        boolean mainItemSame = ItemStack.isSameItem(lastMainHandItem, currentMain);
        boolean mainNameSame = lastMainHandItem.getHoverName().getString().equals(currentMain.getHoverName().getString());
        boolean offItemSame = ItemStack.isSameItem(lastOffHandItem, currentOff);
        boolean offNameSame = lastOffHandItem.getHoverName().getString().equals(currentOff.getHoverName().getString());

        if (!mainItemSame || !mainNameSame || !offItemSame || !offNameSame) {
            lastMainHandItem = currentMain.copy();
            lastOffHandItem = currentOff.copy();
            // 切武器打断检视：只停捕获的检视音效，绝不触碰其他音效（如新武器的 draw 切出音效）
            stopInspectSound();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onKey(InputEvent.Key event) {
        if (inspectKeyMapping != null && event.getAction() == 1 && inspectKeyMapping.matches(event.getKey(), event.getScanCode())) {
            // 连续按检视键防叠加：停掉上一次的检视音效（TaCZ 本次的新检视音效尚未播放）
            stopInspectSound();
            lastInspectPressTime = System.currentTimeMillis();
            captureInspectPending = true;
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

        // 攻击/开镜打断检视：只停检视音效，不影响射击等音效
        stopInspectSound();
    }

    /** 停止捕获的检视音效（若为 null 则无操作） */
    private void stopInspectSound() {
        if (currentInspectSound != null) {
            try {
                stopSoundInstance(currentInspectSound);
            } catch (Exception ignored) {}
            currentInspectSound = null;
        }
    }

    /** 反射读取 SoundPlayManager.tmpSoundInstance */
    private Object readTmpSoundInstance() {
        if (soundManagerClass == null) return null;
        try {
            Field f = soundManagerClass.getDeclaredField("tmpSoundInstance");
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** 调用 GunSoundInstance.setStop()（public 或私有均可） */
    private static void stopSoundInstance(Object instance) throws Exception {
        Method m = instance.getClass().getDeclaredMethod("setStop");
        m.setAccessible(true);
        m.invoke(instance);
    }
}
