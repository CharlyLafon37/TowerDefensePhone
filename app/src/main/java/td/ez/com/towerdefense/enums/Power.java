package td.ez.com.towerdefense.enums;

import td.ez.com.towerdefense.R;

/**
 * Created by Charly on 26/01/2018.
 */

public enum Power
{
    FIRE(R.drawable.fire, R.drawable.fire_disabled, R.raw.fire, "fire"),
    ICE(R.drawable.ice, R.drawable.ice_disabled, R.raw.ice, "ice"),
    THUNDER(R.drawable.thunder, R.drawable.thunder_disabled, R.raw.thunder, "thunder");

    private int resPowerEnabledDrawable;
    private int resPowerDisabledDrawable;
    private int resSoundEffect;
    private String power;

    Power(int resPowerEnabledDrawable, int resPowerDisabledDrawable, int resSoundEffect, String power)
    {
        this.resPowerEnabledDrawable = resPowerEnabledDrawable;
        this.resPowerDisabledDrawable = resPowerDisabledDrawable;
        this.resSoundEffect = resSoundEffect;
        this.power = power;
    }

    public int getPowerEnabledDrawable()
    {
        return resPowerEnabledDrawable;
    }

    public int getPowerDisabledDrawable()
    {
        return resPowerDisabledDrawable;
    }

    public int getResSoundEffect() { return resSoundEffect; }

    public String toString() { return power; }
}
