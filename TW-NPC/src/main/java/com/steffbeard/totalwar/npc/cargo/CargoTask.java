package com.steffbeard.totalwar.npc.cargo;

import java.util.Arrays;
import java.util.Collections;

import net.countercraft.movecraft.utils.HashHitBox;
import nl.thewgbbroz.dtltraders.guis.tradegui.items.TradableGUIItem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.steffbeard.totalwar.npc.Config;
import com.steffbeard.totalwar.npc.NPCMain;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;

public abstract class CargoTask extends BukkitRunnable {
	
	private Config config;
	
    protected Craft craft;
    protected final HashHitBox originalLocations;
    protected final Player originalPilot;
    protected TradableGUIItem item;
    //protected Stock stock;
    //protected final StockItem item;
    protected CargoTask(Craft craft, TradableGUIItem guiItem){
        if (craft == null) {
            throw new IllegalArgumentException("craft must not be null");
        }
        else
            this.craft = craft;
        if (guiItem == null)
            throw new IllegalArgumentException("item must not be null");
        else
            this.item = guiItem;
        this.originalLocations = craft.getHitBox();
        this.originalPilot = CraftManager.getInstance().getPlayerFromCraft(craft);
    }

    @Override
    public void run() {
        if (CraftManager.getInstance().getCraftByPlayer(originalPilot)==null){
            if(config.debugMode)
                NPCMain.logger.info("canceling CargoTask due to missing player/craft");
            NPCMain.getQue().remove(originalPilot);
            this.cancel();
            return;
        }

        if (CraftManager.getInstance().getPlayerFromCraft(craft)!=originalPilot ){
            originalPilot.sendMessage("Pilots changed!");
            NPCMain.getQue().remove(originalPilot);
            this.cancel();
            return;
        }

        if(craft.getHitBox() != originalLocations) {
            originalPilot.sendMessage("Blocks moved/changed!");
            NPCMain.getQue().remove(originalPilot);
            this.cancel();
            return;
        }
        if(config.debugMode)
            NPCMain.logger.info("Running execute method for CargoTask with address " + this + ". Pilot: " + originalPilot.getName() + " CraftSize: " + originalLocations.size() + " CraftType: " + craft.getType().getCraftName() + " StockItem: " + (item.getDisplayName().length() > 0 ? item.getDisplayName() : item.getMainItem().getType().name().toLowerCase()));
        execute();
    }

    protected abstract void execute();
}
