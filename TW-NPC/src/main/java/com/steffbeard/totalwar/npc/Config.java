package com.steffbeard.totalwar.npc;

import java.io.File;
import java.util.Arrays;

import com.steffbeard.totalwar.npc.utils.ConfigManager;

public class Config extends ConfigManager {
	
	@ConfigOptions(name = "scan.range")
	public double scanRange;
	
	@ConfigOptions(name = "transfer.delay.ticks")
	public int transferDelay;
	
	@ConfigOptions(name = "load.tax.percent")
	public double loadTax;
	
	@ConfigOptions(name = "unload.tax.percent")
	public double unloadTax;
	
	@ConfigOptions(name = "cardinal.distance")
	public boolean cardinalDistance;
	
	@ConfigOptions(name = "debug.mode")
	public boolean debugMode;

	protected Config(final File dataFolder) {
		
        super(new File(dataFolder, "config.yml"), Arrays.asList("Cargo NPC", "Configuration for Cargo NPC type."));
        
        this.scanRange = 100.0;
        this.transferDelay = 300;
        this.loadTax = 0.01D;
        this.unloadTax = 0.01D;
        this.cardinalDistance = true;
        this.debugMode = false;
	}

}
