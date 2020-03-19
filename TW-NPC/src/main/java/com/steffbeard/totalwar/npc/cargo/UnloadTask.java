package com.steffbeard.totalwar.npc.cargo;

import net.countercraft.movecraft.craft.Craft;
import nl.thewgbbroz.dtltraders.guis.tradegui.items.TradableGUIItem;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import com.steffbeard.totalwar.npc.Config;
import com.steffbeard.totalwar.npc.NPCMain;
import com.steffbeard.totalwar.npc.utils.CargoUtils;

import java.util.List;

public class UnloadTask extends CargoTask {
   
	public Config config;
	
	public UnloadTask(Craft craft, TradableGUIItem item){
        super(craft,item);
    }

    public void execute(){
        List<Inventory> invs = CargoUtils.getInventories(craft, item.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST);
        Inventory inv = invs.get(0);
        int count = 0;
        for(int i = 0; i<inv.getSize();i++){
            if(inv.getItem(i) != null && inv.getItem(i).isSimilar(item.getMainItem())){
                count+=inv.getItem(i).getAmount();
                inv.setItem(i,null);
            }
        }
        originalPilot.sendMessage(NPCMain.SUCCESS_TAG + "Unloaded " + count + " worth $" + String.format("%.2f", count*item.getTradePrice()) + " took a tax of " + String.format("%.2f",config.unloadTax*count*item.getTradePrice()));
        NPCMain.getEconomy().depositPlayer(originalPilot,count*item.getTradePrice()*(1-config.unloadTax));

        if(invs.size()<=1){
            this.cancel();
            NPCMain.getQue().remove(originalPilot);
            originalPilot.sendMessage(NPCMain.SUCCESS_TAG + "All cargo unloaded");
            return;
        }
        new ProcessingTask(originalPilot, item,invs.size()).runTaskTimer(NPCMain.getInstance(),0,20);
    }
}
