package com.minkang.wild;

import org.bukkit.*;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomTeleporter {
    private final Random rng = new Random();
    private static final Set<Material> NOT_GROUND = new HashSet<>();

    static {
        NOT_GROUND.add(Material.WATER);
        NOT_GROUND.add(Material.LAVA);
        NOT_GROUND.add(Material.CACTUS);
        NOT_GROUND.add(Material.FIRE);
        NOT_GROUND.add(Material.CAMPFIRE);
        NOT_GROUND.add(Material.MAGMA_BLOCK);
        NOT_GROUND.add(Material.SWEET_BERRY_BUSH);
        for (Material m : Material.values()) {
            String n = m.name();
            if (n.endsWith("_LEAVES") || n.endsWith("_GLASS") || n.endsWith("_ICE")) {
                NOT_GROUND.add(m);
            }
        }
    }

    public Location findSafe(RandomWildPlugin plugin, World world) {
        int min = Math.max(0, plugin.cfg().getInt("min-radius", 200));
        int max = Math.max(min + 1, plugin.cfg().getInt("max-radius", 5000));
        int attempts = Math.max(1, plugin.cfg().getInt("max-attempts", 120));

        int phases = Math.max(1, plugin.cfg().getInt("search.phases", 3));
        double grow = plugin.cfg().getDouble("search.phase-radius-grow", 0.5);
        int downDepth = Math.max(0, plugin.cfg().getInt("search.downward-check-depth", 8));

        @SuppressWarnings("unchecked")
        List<String> unsafe = (List<String>) plugin.cfg().getList("unsafe-blocks");

        Location center = world.getSpawnLocation();
        int triesPerPhase = Math.max(1, attempts / phases);
        int leftover = Math.max(0, attempts - triesPerPhase * phases);

        for (int ph = 0; ph < phases; ph++) {
            double scale = 1.0 + (grow * ph);
            int pMin = (int)Math.round(min * scale);
            int pMax = (int)Math.round(max * scale);

            int tries = triesPerPhase + (ph == phases - 1 ? leftover : 0);

            for (int i = 0; i < tries; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                int dist = pMin + rng.nextInt(pMax - pMin + 1);
                int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
                int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);

                int surfaceY = world.getHighestBlockYAt(x, z);
                if (surfaceY <= world.getMinHeight()) continue;

                Location candidate = checkColumn(world, x, z, surfaceY, downDepth, unsafe);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private Location checkColumn(World world, int x, int z, int surfaceY, int downDepth, List<String> blacklist) {
        for (int dy = 0; dy <= downDepth; dy++) {
            int y = surfaceY - dy;
            if (y < world.getMinHeight() || y + 2 > world.getMaxHeight()) break;

            Block below = world.getBlockAt(x, y, z);
            Block feet  = world.getBlockAt(x, y + 1, z);
            Block head  = world.getBlockAt(x, y + 2, z);

            if (!feet.getType().isAir() || !head.getType().isAir()) continue;
            if (!isSolidGround(below.getType(), blacklist)) continue;

            return new Location(world, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    private boolean isSolidGround(Material mat, List<String> blacklist) {
        if (mat == null) return false;
        if (mat.isAir()) return false;
        if (NOT_GROUND.contains(mat)) return false;
        if (blacklist != null) {
            for (String s : blacklist) {
                try { if (mat == Material.valueOf(s)) return false; } catch (IllegalArgumentException ignored) {}
            }
        }
        return mat.isSolid();
    }
}
