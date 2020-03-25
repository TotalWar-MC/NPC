package com.steffbeard.totalwar.npc.cargo;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
/**
* CargoTrait is a marker trait that needs to be added to all dtl traders that can interact with Movecraft crafts
*/
@TraitName("cargo")
public class CargoTrait extends Trait {

    public CargoTrait() {
        super("cargo");
    }

    @Override
    public void onAttach(){
        if (!npc.hasTrait(nl.thewgbbroz.dtltraders.citizens.TraderTrait.class))
            npc.removeTrait(CargoTrait.class);
    }

}
