package td.ez.com.towerdefense.enums;

import android.graphics.drawable.Drawable;

import td.ez.com.towerdefense.R;

/**
 * Created by Charly on 26/01/2018.
 */

public enum Power
{
    FIRE(R.drawable.fire, R.drawable.fire_disabled, "fire"),
    ICE(R.drawable.ice, R.drawable.ice_disabled, "ice"),
    THUNDER(R.drawable.thunder, R.drawable.thunder_disabled, "thunder");

    private int resPowerEnabledDrawable;
    private int resPowerDisabledDrawable;
    private String power;

    Power(int resPowerEnabledDrawable, int resPowerDisabledDrawable, String power)
    {
        this.resPowerEnabledDrawable = resPowerEnabledDrawable;
        this.resPowerDisabledDrawable = resPowerDisabledDrawable;
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

    public String toString() { return power; }
}
