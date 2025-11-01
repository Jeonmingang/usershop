
package com.github.pixeldungeon.manager;

import com.github.pixeldungeon.PixelDungeonPlugin;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

/**
 * Safe runtime bridge for Pixelmon/Forge (no compile-time dependency).
 * - If Forge/Pixelmon are present, we attach simple listeners by reflection.
 * - Identify dungeon-spawned pokemon by name prefix from config (spawnNamePrefix).
 * - On capture events involving dungeon mobs, cancel.
 * - On battle start involving dungeon mobs, try to disable bag usage.
 *
 * If any step fails at runtime, it silently no-ops.
 */
public final class PixelmonRuntimeBridge {
    private final PixelDungeonPlugin plugin;
    private Object eventBus; // MinecraftForge.EVENT_BUS
    private boolean installed = false;

    public PixelmonRuntimeBridge(PixelDungeonPlugin plugin){ this.plugin = plugin; }

    public void tryInstall(){
        try {
            Class<?> forge = Class.forName("net.minecraftforge.common.MinecraftForge");
            Field f = forge.getField("EVENT_BUS");
            eventBus = f.get(null);
            if (eventBus == null) return;

            // Find EventBus#addListener(Consumer)
            Method addListener = null;
            for (Method m : eventBus.getClass().getMethods()) {
                if (m.getName().equals("addListener") && m.getParameterCount() == 1) {
                    Class<?> p0 = m.getParameterTypes()[0];
                    if (java.util.function.Consumer.class.isAssignableFrom(p0)) { addListener = m; break; }
                }
            }
            if (addListener == null) return;

            // Listener: capture
            Consumer<Object> onCapture = new Consumer<Object>() {
                @Override public void accept(Object ev) {
                    try {
                        String cn = ev.getClass().getName().toLowerCase();
                        if (!cn.contains("pixelmon") || !(cn.contains("capture")||cn.contains("throw")||cn.contains("pokeball"))) return;
                        Object pokemon = invokeFirst(ev, "getPokemon");
                        Object ent = (pokemon != null ? invokeFirst(pokemon, "getEntity") : null);
                        if (ent == null) return;
                        if (isDungeonByName(ent)) {
                            // cancel if cancelable
                            try {
                                Method cancel = ev.getClass().getMethod("setCanceled", boolean.class);
                                cancel.invoke(ev, true);
                            } catch (Throwable ignored) {}
                        }
                    } catch (Throwable ignored) {}
                }
            };

            // Listener: battle start (disable bag)
            Consumer<Object> onBattle = new Consumer<Object>() {
                @Override public void accept(Object ev) {
                    try {
                        String cn = ev.getClass().getName().toLowerCase();
                        if (!cn.contains("pixelmon") || !cn.contains("battle")) return;
                        Object battle = invokeFirst(ev, "getBattle", "getBattleController");
                        if (battle == null) return;

                        boolean relevant = false;
                        List<Object> parts = collectParticipants(battle);
                        for (Object p : parts) {
                            Object ent = invokeFirst(p, "getEntity", "getPokemon", "getPokemonEntity");
                            if (ent == null) ent = invokeFirst(p, "getEntity");
                            if (ent != null && isDungeonByName(ent)) { relevant = true; break; }
                        }
                        if (!relevant) return;

                        Object rules = invokeFirst(battle, "getRules");
                        if (rules != null) {
                            if (!invokeTrySet(rules, "setCanUseBag", new Class<?>[]{boolean.class}, new Object[]{false})) {
                                trySetField(rules, "bagAllowed", false);
                                    trySetField(rules, "allowBag", false);
                                    trySetField(rules, "canThrowPokeballs", false);
                                    trySetField(rules, "canCatch", false);
                                    trySetField(rules, "catchAllowed", false);
                                    trySetField(rules, "noCatch", true);
                                trySetField(rules, "canUseBag", false);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            };

            addListener.invoke(eventBus, onCapture);
            addListener.invoke(eventBus, onBattle);
            installed = true;
            Bukkit.getLogger().info("[PixelDungeon] PixelmonRuntimeBridge installed.");
        } catch (Throwable ignored) {
            // Pixelmon/Forge not present or incompatible; no-op.
        }
    }

    private boolean isDungeonByName(Object ent){
        try {
            String prefix = plugin.getConfig().getString("spawnNamePrefix","PD-");
            Object nameObj = invokeFirst(ent, "getName", "getCustomName", "getNickname");
            String name = (nameObj != null ? String.valueOf(nameObj) : null);
            return name != null && name.startsWith(prefix);
        } catch (Throwable e) {
            return false;
        }
    }

    // ---------- small reflection helpers ----------
    private static Object invokeFirst(Object o, String... names){
        if (o == null) return null;
        for (String n : names) {
            try {
                Method m = o.getClass().getMethod(n);
                return m.invoke(o);
            } catch (Throwable ignored) {}
        }
        return null;
    }
    private static boolean invokeTrySet(Object o, String name, Class<?>[] types, Object[] args){
        try {
            Method m = o.getClass().getMethod(name, types);
            m.invoke(o, args);
            return true;
        } catch (Throwable e) { return false; }
    }
    private static void trySetField(Object o, String f, Object v){
        try {
            java.lang.reflect.Field fld = o.getClass().getDeclaredField(f);
            fld.setAccessible(true);
            fld.set(o, v);
        } catch (Throwable ignored) {}
    }
    @SuppressWarnings("unchecked")
    private static List<Object> collectParticipants(Object battle){
        try {
            Object list = invokeFirst(battle, "getParticipants", "getTeamOne", "getTeamTwo");
            if (list instanceof java.util.List) return (java.util.List<Object>) list;
        } catch (Throwable ignored) {}
        return java.util.Collections.emptyList();
    }
}
