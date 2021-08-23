package com.d2magicos.carrobluetooth;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ThreadActivity extends AppCompatActivity {

    boolean bolEmpezar = false;
    int contador = 0;
    TextView txtNumero;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_thread);

        txtNumero = findViewById(R.id.txt_numero);
    }

    public void iniciarHilo(View view){

        bolEmpezar = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (bolEmpezar){
                    contador++;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtNumero.setText(String.valueOf(contador));
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

}
