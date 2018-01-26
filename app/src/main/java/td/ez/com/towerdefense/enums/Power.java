package td.ez.com.towerdefense.enums;

import android.graphics.drawable.Drawable;

import td.ez.com.towerdefense.R;

/**
 * Created by Charly on 26/01/2018.
 */

public enum Power
{
    FIRE(R.drawable.fire, R.drawable.fire_disabled),
    ICE(R.drawable.ice, R.drawable.ice_disabled),
    THUNDER(R.drawable.thunder, R.drawable.thunder_disabled);

    private int resPowerEnabledDrawable;
    private int resPowerDisabledDrawable;

    Power(int resPowerEnabledDrawable, int resPowerDisabledDrawable)
    {
        this.resPowerEnabledDrawable = resPowerEnabledDrawable;
        this.resPowerDisabledDrawable = resPowerDisabledDrawable;
    }

    public int getPowerEnabledDrawable()
    {
        return resPowerEnabledDrawable;
    }

    public int getPowerDisabledDrawable()
    {
        return resPowerDisabledDrawable;
    }
}
