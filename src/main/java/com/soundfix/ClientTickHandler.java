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
    private Method stopAllMethod;
    private KeyMapping inspectKeyMapping;
    private long lastInspectPressTime = 0;
    /** 检视键按下时的追踪池快照，用于识别随后新增的检视音效 */
    private Set<Object> candidateBaseline = null;
    /** 检视音效候选：检视键按下后新进入追踪池的音效实例 */
    private final Set<Object> inspectCandidates = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final long INSPECT_TIMEOUT = 8000;

    public ClientTickHandler() {
        try {
            soundManagerClass = Class.forName("com.tacz.guns.client.sound.SoundPlayManager");
            stopAllMethod = soundManagerClass.getDeclaredMethod("stopAndClearTrackedSounds");
            stopAllMethod.setAccessible(true);
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
        // 增量收集检视音效候选：快照之后新进入追踪池的音效视为本次检视音效
        if (candidateBaseline != null) {
            for (Object inst : collectTrackedInstances()) {
                if (!candidateBaseline.contains(inst)) {
                    inspectCandidates.add(inst);
                }
            }
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
            // 切武器打断检视：只停检视音效候选，不触碰 draw/射击等其他音效
            stopInspectCandidates();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onKey(InputEvent.Key event) {
        if (inspectKeyMapping != null && event.getAction() == 1 && inspectKeyMapping.matches(event.getKey(), event.getScanCode())) {
            // 按检视键：全停旧音效（本次新检视音效尚未播放，安全）防止叠加
            stopAllSounds();
            lastInspectPressTime = System.currentTimeMillis();
            // 快照当前追踪池（全停后通常为空），之后新增的音效即为本次检视音效
            candidateBaseline = collectTrackedInstances();
            inspectCandidates.clear();
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

        // 攻击/开镜打断检视：只停检视音效候选，不影响射击等音效
        stopInspectCandidates();
    }

    private void stopAllSounds() {
        if (stopAllMethod != null) {
            try {
                stopAllMethod.invoke(null);
            } catch (Exception ignored) {}
        }
    }

    private void stopInspectCandidates() {
        for (Object inst : inspectCandidates) {
            try {
                stopSoundInstance(inst);
            } catch (Exception ignored) {}
        }
        inspectCandidates.clear();
        candidateBaseline = null;
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
