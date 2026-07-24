package com.csykes.searchlight;

import com.csykes.searchlight.features.teddy.TeddyBearEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = "searchlight", bus = EventBusSubscriber.Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Attach the attributes to the entity type
        event.put(Searchlight.TEDDY_BEAR.get(), TeddyBearEntity.createAttributes().build());
    }
}
