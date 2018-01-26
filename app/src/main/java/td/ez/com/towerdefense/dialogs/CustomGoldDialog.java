package td.ez.com.towerdefense.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import td.ez.com.towerdefense.R;

/**
 * Created by Charly on 25/01/2018.
 */

public class CustomGoldDialog extends DialogFragment
{
    private List<String> names;
    private int currentGold;

    public interface CustomGoldListener extends Serializable
    {
        void onPositiveClick(String playerName, int amount);
    }

    private CustomGoldListener listener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        names = bundle.getStringArrayList("names");
        currentGold = bundle.getInt("currentGold");
        listener = (CustomGoldListener) bundle.get("listener");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_customgold_title));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View inflation = inflater.inflate(R.layout.dialog_customgold, null);
        builder.setView(inflation);

        final RadioButton radioButton1 = inflation.findViewById(R.id.customgold_name1);
        final RadioButton radioButton2 = inflation.findViewById(R.id.customgold_name2);
        int i = 0;
        for(String name : names)
        {
            if(i == 0)
                radioButton1.setText(name);
            else if(i == 1)
                radioButton2.setText(name);
            i++;
        }

        builder.setNegativeButton("Annuler", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                CustomGoldDialog.this.getDialog().cancel();
            }
        });
        builder.setPositiveButton("Envoyer", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                EditText amountView = inflation.findViewById(R.id.customgold_amount);
                int amount = 0;
                try
                {
                    amount = Integer.parseInt(amountView.getText().toString().trim());
                }
                catch(NumberFormatException e)
                {
                    return;
                }
                if(amount > currentGold || amount < 0)
                {
                    return;
                }

                String player;
                RadioGroup group = inflation.findViewById(R.id.customgold_listnames);
                switch(group.getCheckedRadioButtonId())
                {
                    case R.id.customgold_name1:
                    {
                        player = radioButton1.getText().toString();
                        break;
                    }
                    case R.id.customgold_name2:
                    {
                        player = radioButton2.getText().toString();
                        break;
                    }
                    default: player = "UNDEFINED";
                }

                listener.onPositiveClick(player, amount);
            }
        });

        return builder.create();
    }
}
