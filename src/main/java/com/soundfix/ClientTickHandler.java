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
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientTickHandler {
    private ItemStack lastMainHandItem = ItemStack.EMPTY;
    private ItemStack lastOffHandItem = ItemStack.EMPTY;
    private Class<?> soundManagerClass;
    private Method stopMethod;
    private KeyMapping inspectKeyMapping;
    private long lastInspectPressTime = 0;
    private static final long INSPECT_TIMEOUT = 8000;

    public ClientTickHandler() {
        try {
            soundManagerClass = Class.forName("com.tacz.guns.client.sound.SoundPlayManager");
            stopMethod = soundManagerClass.getDeclaredMethod("stopAndClearTrackedSounds");
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
    public void onClientTick(ClientTickEvent.Post event) {
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
            // 物品变化（切武器）：只停止旧的追踪音效（如检视），
            // 跳过 tmpSoundInstance —— 切武器时它指向刚播放的 draw 音效，不能误停。
            stopTrackedSoundsExceptCurrent();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onKey(InputEvent.Key event) {
        if (inspectKeyMapping != null && event.getAction() == 1 && inspectKeyMapping.matches(event.getKey(), event.getScanCode())) {
            // 按检视键：新检视音效尚未播放，全停旧音效防止叠加
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

        // 攻击/开镜打断检视：射击音效此时尚未播放（TaCZ 在 tick 中处理开火），全停安全
        stopSounds();
    }

    private void stopSounds() {
        if (stopMethod != null) {
            try {
                stopMethod.invoke(null);
            } catch (Exception ignored) {}
        }
    }

    /**
     * 停止 TRACKED_GUN_SOUNDS 中除 tmpSoundInstance 外的所有音效。
     * 切武器时 tmpSoundInstance 已被 TaCZ 覆盖为新武器的 draw 音效，需跳过以免误停。
     * 若反射失败（TaCZ 结构变化）则回退到全停。
     */
    private void stopTrackedSoundsExceptCurrent() {
        if (soundManagerClass == null) return;
        try {
            Field tmpField = soundManagerClass.getDeclaredField("tmpSoundInstance");
            tmpField.setAccessible(true);
            Object current = tmpField.get(null);

            Field trackedField = soundManagerClass.getDeclaredField("TRACKED_GUN_SOUNDS");
            trackedField.setAccessible(true);
            Object mapObj = trackedField.get(null);
            if (!(mapObj instanceof Map<?, ?> map)) {
                stopSounds();
                return;
            }
            boolean stoppedAny = false;
            for (Object dequeObj : map.values()) {
                if (!(dequeObj instanceof Deque<?> deque)) continue;
                Iterator<?> it = deque.iterator();
                while (it.hasNext()) {
                    Object tracked = it.next();
                    // TrackedGunSound record 的 instance() 组件
                    Object instance = getRecordComponent(tracked, "instance");
                    if (instance == null || instance == current) continue;
                    stopSoundInstance(instance);
                    it.remove();
                    stoppedAny = true;
                }
            }
            if (!stoppedAny) {
                // 追踪池里没有可停的（例如新武器 draw 已入池但被跳过）——无需全停
            }
        } catch (Exception e) {
            stopSounds();
        }
    }

    private static Object getRecordComponent(Object recordObj, String componentName) throws Exception {
        Method m = recordObj.getClass().getDeclaredMethod(componentName);
        m.setAccessible(true);
        return m.invoke(recordObj);
    }

    private static void stopSoundInstance(Object instance) throws Exception {
        Method m = instance.getClass().getDeclaredMethod("setStop");
        m.setAccessible(true);
        m.invoke(instance);
    }
}
