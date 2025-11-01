package com.github.pixeldungeon.model;

import java.util.ArrayList;
import java.util.List;

public class Stage {
    public int index;
    public Region region;
    public List<SpawnSpec> spawns = new ArrayList<>();

    // AND model: must kill all within time
    public boolean killAll = true;
    public boolean timeEnabled = true;
    public int timeSeconds = 60;

    // legacy placeholders kept for config compatibility
    public String objective = "COMBINED";
    public int waveIntervalSeconds = 10;
}
