package com.d2magicos.carrobluetooth.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Config {

    private static Toast toastMessage;


    @SuppressLint("ShowToast")
    public static void Mensaje(Context context, String message, Boolean longToast, Boolean isWarning){
        if(toastMessage == null){
            toastMessage = Toast.makeText(context.getApplicationContext(), "", Toast.LENGTH_SHORT);
            toastMessage.setGravity(Gravity.FILL_HORIZONTAL|Gravity.TOP, 0, 120);
            View view = toastMessage.getView();
            view.setBackgroundResource(android.R.drawable.toast_frame);
            TextView toastMessageText = view.findViewById(android.R.id.message);
            toastMessageText.setTextColor(Color.parseColor("#ffffff"));
        }

        if(isWarning){
            toastMessage.getView().setBackgroundColor(Color.parseColor("#d50000"));
        }else{
            toastMessage.getView().setBackgroundColor(Color.parseColor("#646464"));
        }

        toastMessage.setDuration(longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toastMessage.setText(message);
        toastMessage.show();
    }
}