package com.steffbeard.totalwar.npc.cargo;

import net.countercraft.movecraft.craft.Craft;
import nl.thewgbbroz.dtltraders.guis.tradegui.items.TradableGUIItem;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.steffbeard.totalwar.npc.Config;
import com.steffbeard.totalwar.npc.NPCMain;
import com.steffbeard.totalwar.npc.utils.CargoUtils;

import java.util.List;

public class LoadTask extends CargoTask {
	
	public Config config;
	
    public LoadTask(Craft craft, TradableGUIItem item){
        super(craft,item);
    }

    protected void execute(){
        List<Inventory> invs = CargoUtils.getInventoriesWithSpace(craft, item.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST);

        Inventory inv = invs.get(0);
        int loaded=0;
        for(int i =0; i < inv.getSize() ; i++)
            if(inv.getItem(i)==null || inv.getItem(i).getType()==Material.AIR || inv.getItem(i).isSimilar(item.getMainItem())){
                int maxCount = (inv.getItem(i)==null || inv.getItem(i).getType()==Material.AIR) ? item.getMainItem().getMaxStackSize() : inv.getItem(i).getMaxStackSize() - inv.getItem(i).getAmount();
                if(NPCMain.getEconomy().getBalance(originalPilot) > item.getTradePrice()*maxCount*(1+config.loadTax)){
                    loaded+=maxCount;
                    ItemStack tempItem = item.getMainItem().clone();
                    tempItem.setAmount(tempItem.getMaxStackSize());
                    inv.setItem(i,tempItem);
                    
                }else{
                    maxCount = (int)(NPCMain.getEconomy().getBalance(originalPilot)/(item.getTradePrice()*(1+config.loadTax)));
                    this.cancel();
                    NPCMain.getQue().remove(originalPilot);
                    originalPilot.sendMessage(NPCMain.SUCCESS_TAG + "You ran out of money!");
                    if(maxCount<=0){
                        if(config.debugMode){
                            NPCMain.logger.info("Balance: " + NPCMain.getEconomy().getBalance(originalPilot) + ". maxCount: " + maxCount + ".");
                        }
                        originalPilot.sendMessage(NPCMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $" + String.format("%.2f", loaded*item.getTradePrice()) + " took a tax of " + String.format("%.2f",config.loadTax*loaded*item.getTradePrice()));
                        return;
                    }
                    ItemStack tempItem = item.getMainItem().clone();
                    if(inv.getItem(i)==null || inv.getItem(i).getType()==Material.AIR) 
                        tempItem.setAmount(maxCount);
                    else
                        tempItem.setAmount(inv.getItem(i).getAmount()+maxCount);
                    inv.setItem(i,tempItem);
                    loaded+=maxCount;
                    if(config.debugMode){
                        NPCMain.logger.info("Balance: " + NPCMain.getEconomy().getBalance(originalPilot) + ". maxCount: " + maxCount + ". Actual stack-size: " + tempItem.getAmount());
                    }
                    originalPilot.sendMessage(NPCMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $" + String.format("%.2f", loaded*item.getTradePrice()) + " took a tax of " + String.format("%.2f",config.loadTax*loaded*item.getTradePrice()));
                    NPCMain.getEconomy().withdrawPlayer(originalPilot,loaded*item.getTradePrice()*(1+config.loadTax));
                    return;
                }
                NPCMain.getEconomy().withdrawPlayer(originalPilot,maxCount*item.getTradePrice()*(1+config.loadTax));
            }

        originalPilot.sendMessage(NPCMain.SUCCESS_TAG + "Loaded " + loaded + " items worth $" + String.format("%.2f", loaded*item.getTradePrice()) + " took a tax of " + String.format("%.2f",config.loadTax*loaded*item.getTradePrice()));
        

        if(invs.size()<= 1){
            this.cancel();
            NPCMain.getQue().remove(originalPilot);
            originalPilot.sendMessage(NPCMain.SUCCESS_TAG + "All cargo loaded");
            return;
        }
        new ProcessingTask(originalPilot, item,invs.size()).runTaskTimer(NPCMain.getInstance(),0,20);
    }
}
