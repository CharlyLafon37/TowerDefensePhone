package td.ez.com.towerdefense.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.io.Serializable;
import java.util.Set;

import td.ez.com.towerdefense.R;

/**
 * Created by Charly on 18/01/2018.
 */

public class PickPlayerDialog extends DialogFragment
{
    private Set<String> names;

    public interface PickPlayerListener extends Serializable
    {
        void onPlayerPicked(String playerName);
    }

    PickPlayerListener listener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        names = (Set) bundle.getSerializable("names");
        listener = (PickPlayerListener) bundle.get("listener");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_pickplayer_title));

        final CharSequence[] namesPlayers = names.toArray(new CharSequence[]{});

        builder.setItems(namesPlayers, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                listener.onPlayerPicked(namesPlayers[i].toString());
            }
        });
        return builder.create();
    }
}
