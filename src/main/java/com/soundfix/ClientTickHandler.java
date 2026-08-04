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
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientTickHandler {
    private ItemStack lastMainHandItem = ItemStack.EMPTY;
    private ItemStack lastOffHandItem = ItemStack.EMPTY;
    private Class<?> soundManagerClass;
    private KeyMapping inspectKeyMapping;
    private long lastInspectPressTime = 0;
    /**
     * 上一客户端 tick 的追踪池快照。
     * 其中的音效视为"旧音效"（检视音效、上一次切换的切出音效），
     * 切武器/攻击/检视时可安全停止；本 tick 新入池的音效（本次切出的 draw）不在快照中，不会被误停。
     */
    private Set<Object> lastTickSounds = Collections.emptySet();
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastTickSounds = Collections.emptySet();
            return;
        }

        ItemStack currentMain = mc.player.getMainHandItem();
        ItemStack currentOff = mc.player.getOffhandItem();

        boolean mainItemSame = ItemStack.isSameItem(lastMainHandItem, currentMain);
        boolean mainNameSame = lastMainHandItem.getHoverName().getString().equals(currentMain.getHoverName().getString());
        boolean offItemSame = ItemStack.isSameItem(lastOffHandItem, currentOff);
        boolean offNameSame = lastOffHandItem.getHoverName().getString().equals(currentOff.getHoverName().getString());

        if (!mainItemSame || !mainNameSame || !offItemSame || !offNameSame) {
            lastMainHandItem = currentMain.copy();
            lastOffHandItem = currentOff.copy();
            // 切武器：停止上一 tick 就在播放的旧音效（检视音效、旧武器的长切出音效），
            // 保留本 tick 刚播放的新音效（新武器的 draw 音效）
            stopOldSounds();
        }

        // 更新快照供下一 tick 使用（在本 tick 新音效入池之后更新，使其成为下一轮的"旧音效"）
        lastTickSounds = collectTrackedInstances();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onKey(InputEvent.Key event) {
        if (inspectKeyMapping != null && event.getAction() == 1 && inspectKeyMapping.matches(event.getKey(), event.getScanCode())) {
            // 连续按检视键防叠加：停掉上次检视音效（TaCZ 本次新检视音效尚未播放）
            stopOldSounds();
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

        // 攻击/开镜打断检视：停上一 tick 的旧音效（检视音效），本 tick 的射击音效不受影响
        stopOldSounds();
    }

    /** 停止上一 tick 快照中的音效（旧音效），并清空快照 */
    private void stopOldSounds() {
        for (Object inst : lastTickSounds) {
            try {
                stopSoundInstance(inst);
            } catch (Exception ignored) {}
        }
        lastTickSounds = Collections.emptySet();
    }

    /** 反射收集 TRACKED_GUN_SOUNDS 中所有音效实例 */
    private Set<Object> collectTrackedInstances() {
        Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
        if (soundManagerClass == null) return result;
        try {
            Field f = soundManagerClass.getDeclaredField("TRACKED_GUN_SOUNDS");
            f.setAccessible(true);
            Object mapObj = f.get(null);
            if (mapObj instanceof Map<?, ?> map) {
                for (Object dequeObj : map.values()) {
                    if (!(dequeObj instanceof Deque<?> deque)) continue;
                    for (Object tracked : deque) {
                        Object instance = getRecordComponent(tracked, "instance");
                        if (instance != null) result.add(instance);
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    /** 读取 TrackedGunSound record 的 instance() 组件 */
    private static Object getRecordComponent(Object recordObj, String componentName) throws Exception {
        Method m = recordObj.getClass().getDeclaredMethod(componentName);
        m.setAccessible(true);
        return m.invoke(recordObj);
    }

    /** 调用 GunSoundInstance.setStop() */
    private static void stopSoundInstance(Object instance) throws Exception {
        Method m = instance.getClass().getDeclaredMethod("setStop");
        m.setAccessible(true);
        m.invoke(instance);
    }
}
