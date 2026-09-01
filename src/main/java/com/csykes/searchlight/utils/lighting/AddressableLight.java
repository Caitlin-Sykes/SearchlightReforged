package com.csykes.searchlight.utils.lighting;

public interface AddressableLight {
    String getAddress();
    void setAddress(String address);

    BrightnessStage getBrightness();
    void setBrightness(BrightnessStage brightness);

    LightRequest getLightRequest();
    void setLightRequest(LightRequest request);
}
