package com.csykes.searchlight.integration.jade;

import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(LightDataComponent.INSTANCE, AbstractLightBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(LightDataComponent.INSTANCE, AbstractLightBlock.class);
    }
}
