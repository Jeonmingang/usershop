
package com.github.pixeldungeon.manager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks spawned mob UUIDs per dungeon instance and fires a callback
 * when all tracked entities are gone (dead, captured, despawned, or left the world).
 */
public final class StageTracker {

    private final JavaPlugin plugin;
    private final Map<UUID, World> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> active = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> loops = new ConcurrentHashMap<>();
    private final Map<UUID, Runnable> onClears = new ConcurrentHashMap<>();

    public StageTracker(JavaPlugin plugin){
        this.plugin = plugin;
    }

    /**
     * Begin tracking for an instance.
     * @param instanceId unique id of dungeon instance
     * @param world world that spawned entities are expected to be in
     * @param initial tracked entity UUIDs
     * @param onCleared callback when all tracked entities are gone
     */
    public void begin(UUID instanceId, World world, Collection<UUID> initial, Runnable onCleared){
        stop(instanceId); // cancel previous if any
        worlds.put(instanceId, world);
        Set<UUID> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
        set.addAll(initial);
        active.put(instanceId, set);
        if (onCleared != null) onClears.put(instanceId, onCleared);

        BukkitTask task = new BukkitRunnable(){
            @Override public void run(){
                World w = worlds.get(instanceId);
                Set<UUID> s = active.get(instanceId);
                if (w == null || s == null){
                    cancel();
                    return;
                }
                if (s.isEmpty()){
                    cancel();
                    Runnable cb = onClears.remove(instanceId);
                    if (cb != null){
                        safeRun(cb);
                    }
                    return;
                }
                // copy to avoid concurrent modification
                List<UUID> toCheck = new ArrayList<>(s);
                for (UUID u : toCheck){
                    Entity e = Bukkit.getEntity(u);
                    boolean remove = false;
                    if (e == null || (w != null && e.getWorld() != w)){
                        remove = true; // vanished / despawned / dimension changed
                    } else if (!e.isValid()){
                        remove = true;
                    } else if (e instanceof LivingEntity){
                        LivingEntity le = (LivingEntity) e;
                        if (le.isDead() || le.getHealth() <= 0.0){
                            remove = true;
                        }
                    } else {
                        // non-living tracked entity shouldn't block progression
                        remove = true;
                    }
                    if (remove){
                        s.remove(u);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
        loops.put(instanceId, task);
    }

    public void add(UUID instanceId, UUID mob){
        active.computeIfAbsent(instanceId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(mob);
    }
    public void remove(UUID instanceId, UUID mob){
        Set<UUID> s = active.get(instanceId);
        if(s!=null) s.remove(mob);
    }
    public int remaining(UUID instanceId){
        Set<UUID> s = active.get(instanceId);
        return s==null? 0 : s.size();
    }
    public boolean isCleared(UUID instanceId){ return remaining(instanceId) == 0; }

    public void stop(UUID instanceId){
        BukkitTask t = loops.remove(instanceId);
        if(t != null) t.cancel();
        active.remove(instanceId);
        worlds.remove(instanceId);
        onClears.remove(instanceId);
    }

    private static void safeRun(Runnable r){
        if(r!=null){
            try{ r.run(); }catch(Throwable ignored){}
        }
    }
}
