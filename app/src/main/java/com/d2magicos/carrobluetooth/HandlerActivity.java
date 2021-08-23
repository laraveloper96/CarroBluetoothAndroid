package com.d2magicos.carrobluetooth;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class HandlerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_handler);

    }

    public void iniciarHandler(View view) {

        Handler mHandler = new Handler(){

            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle datos = msg.getData();
                Toast.makeText(HandlerActivity.this, datos.getString("key_msg"), Toast.LENGTH_LONG)
                        .show();
            }
        };

        Message msg = new Message();
        Bundle datos = new Bundle();
        datos.putString("key_msg", "Hola soy el mensaje");
        msg.setData(datos);
        mHandler.sendMessage(msg);

    }

}
