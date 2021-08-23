package com.d2magicos.carrobluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.d2magicos.carrobluetooth.event.UiToastEvent;
import com.d2magicos.carrobluetooth.helper.EnhancedSharedPreferences;
import com.d2magicos.carrobluetooth.helper.NotificationHelper;
import com.d2magicos.carrobluetooth.service.MyBluetoothSerialService;
import com.d2magicos.carrobluetooth.util.Config;
import com.d2magicos.carrobluetooth.util.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class PanelActivity extends AppCompatActivity {

    private PanelActivity.MyServiceMessageHandler myServiceMessageHandler;
    protected MyBluetoothSerialService myBluetoothSerialService = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private boolean mBoundService = false;
    private String mConnectedDeviceName = null;
    private EnhancedSharedPreferences sharedPref;


    private ImageView imgUp, imgDown, imgLight, imgLeft, imgRight, imgStop, imgConnect;

    final static String UP="F";
    final static String DOWN="B";
    final static String LEFT="L";
    final static String RIGHT="R";
    final static String AUTOMATIC="A";
    final static String STOP="S";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.createChannels();

        // check support
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Config.Mensaje(this, getString(R.string.text_no_bluetooth_adapter), false, false);
        }else{
            Intent intent = new Intent(getApplicationContext(), MyBluetoothSerialService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        /**
         * Creamos el hilo para responder a los siguientes estados
         * MESSAGE_STATE_CHANGE
         * MESSAGE_DEVICE_NAME
         * MESSAGE_TOAST
         */
        myServiceMessageHandler = new PanelActivity.MyServiceMessageHandler(this, this);

        inicializarControles();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        sharedPref = EnhancedSharedPreferences.getInstance(getApplicationContext(), getString(R.string.shared_preference_key));
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            //Esto se llama cuando la conexión con el servicio ha sido
            // establecido, dándonos el objeto de servicio que podemos usar para
            // interactuar con el servicio
            MyBluetoothSerialService.MySerialServiceBinder binder = (MyBluetoothSerialService.MySerialServiceBinder) service;
            myBluetoothSerialService = binder.getService(); //Obtenermos la vinculacion del servicio
            mBoundService = true; //Variable para saber que el servicio esta conectado
            myBluetoothSerialService.setMessageHandler(myServiceMessageHandler); //Seteamos el hilo principal
            myBluetoothSerialService.setStatusUpdatePoolInterval(
                    Long.parseLong(sharedPref.getString(getString(
                            R.string.preference_update_pool_interval),
                            String.valueOf(Constants.STATUS_UPDATE_INTERVAL)))); // indicamos con cuanta frecuencia va a realiza actualizaciones el servicio
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Esto se llama cuando la conexión con el servicio ha sido
            // desconectado inesperadamente, es decir, su proceso se bloqueó.
            mBoundService = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        //Activamos el bluetooth por default
        if(!bluetoothAdapter.isEnabled()){
            Thread thread = new Thread(){
                @Override
                public void run(){
                    try{
                        bluetoothAdapter.enable(); //Activamos el BT
                    }catch (RuntimeException e){
                        EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_no_bluetooth_permission), true, true));
                    }
                }
            };
            thread.start();
        }

        //Preguntamos por el estado actual del servicio
        if(myBluetoothSerialService != null) onBluetoothStateChange(myBluetoothSerialService.getState());
    }

    private static class MyServiceMessageHandler extends Handler {

        private final WeakReference<PanelActivity> mActivity;
        private final Context mContext;

        MyServiceMessageHandler(Context context, PanelActivity activity){
            mContext = context;
            mActivity = new WeakReference<>(activity);
        }

        //Recibimos los datos enviados
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    mActivity.get().onBluetoothStateChange(msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    mActivity.get().mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Config.Mensaje(mContext,mActivity.get().getString(R.string.text_connected_to) + " " + mActivity.get().mConnectedDeviceName, false, false);
                    break;
                case Constants.MESSAGE_TOAST:
                    Config.Mensaje(mContext, msg.getData().getString(Constants.TOAST), false, false);
                    break;
            }
        }
    }


    private void onBluetoothStateChange(int currentState){

        switch (currentState){
            case MyBluetoothSerialService.STATE_CONNECTED:
                //Esta conectado
                break;
            case MyBluetoothSerialService.STATE_CONNECTING:
                //Esta conectandose
                break;
            case MyBluetoothSerialService.STATE_LISTEN:
                //Recibiendo datos
                break;
            case MyBluetoothSerialService.STATE_NONE:
                //Indicar que no esta conectado el bluetooth
                break;
        }

    }

    private void inicializarControles() {

        imgUp = findViewById(R.id.img_up);
        imgDown = findViewById(R.id.img_down);
        imgLeft = findViewById(R.id.img_left);
        imgRight = findViewById(R.id.img_right);
        imgLight = findViewById(R.id.img_light);
        imgStop = findViewById(R.id.img_stop);
        imgConnect = findViewById(R.id.img_connect);

        imgConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bluetoothAdapter.isEnabled()){
                    if(myBluetoothSerialService != null){
                        if(myBluetoothSerialService.getState() == myBluetoothSerialService.STATE_CONNECTED){
                            new AlertDialog.Builder(PanelActivity.this)
                                    .setTitle(R.string.text_disconnect)
                                    .setMessage(getString(R.string.text_disconnect_confirm))
                                    .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if(myBluetoothSerialService != null) myBluetoothSerialService.disconnectService();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.text_cancel), null)
                                    .show();

                        }else{
                            Intent serverIntent = new Intent(PanelActivity.this, DeviceListActivity.class);
                            startActivityForResult(serverIntent, Constants.CONNECT_DEVICE_SECURE);
                        }
                    }else{
                        EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_bt_service_not_running), true, true));
                    }

                }else{
                    Config.Mensaje(PanelActivity.this, getString(R.string.text_bt_not_enabled), false, false);
                }
            }
        });

        imgUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == MyBluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(UP); // Enviamos la data a nuestro arduino
                }else{
                    Config.Mensaje(PanelActivity.this, "Debe conectar su bluetooth", false, false);
                }
            }
        });

        imgDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == MyBluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(DOWN); // Enviamos la data a nuestro arduino
                }else{
                    Config.Mensaje(PanelActivity.this, "Debe conectar su bluetooth", false, false);
                }
            }
        });

        imgLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == MyBluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(LEFT); // Enviamos la data a nuestro arduino
                }else{
                    Config.Mensaje(PanelActivity.this, "Debe conectar su bluetooth", false, false);
                }
            }
        });

        imgRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == MyBluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(RIGHT); // Enviamos la data a nuestro arduino
                }else{
                    Config.Mensaje(PanelActivity.this, "Debe conectar su bluetooth", false, false);
                }
            }
        });

        imgLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == MyBluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(AUTOMATIC); // Enviamos la data a nuestro arduino
                }else{
                    Config.Mensaje(PanelActivity.this, "Debe conectar su bluetooth", false, false);
                }
            }
        });

        imgStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == MyBluetoothSerialService.STATE_CONNECTED){
                    myBluetoothSerialService.serialWriteString(STOP); // Enviamos la data a nuestro arduino
                }else{
                    Config.Mensaje(PanelActivity.this, "Debe conectar su bluetooth", false, false);
                }
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Constants.CONNECT_DEVICE_INSECURE:
            case Constants.CONNECT_DEVICE_SECURE:
                if(resultCode == Activity.RESULT_OK){

                    mConnectedDeviceName = Objects.requireNonNull(data.getExtras()).getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    sharedPref.edit().putString(getString(R.string.preference_last_connected_device), mConnectedDeviceName).apply();
                    Log.e("MI_DATO", ""+mConnectedDeviceName);
                    connectToDevice(mConnectedDeviceName);
                }
        }

    }

    private void connectToDevice(String macAddress){
        if(macAddress == null){
            //Si el nombre es nulo entonces volvemos a mostrar la lista de dispositivos para que se vuelva a conectar
            Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
            startActivityForResult(serverIntent, Constants.CONNECT_DEVICE_SECURE);
        }else{
            ;Intent intent = new Intent(getApplicationContext(), MyBluetoothSerialService.class);
            intent.putExtra(MyBluetoothSerialService.KEY_MAC_ADDRESS, macAddress);

            //Verificamos que sea la version 26(Oreo) a superior esto se debe
            // a las limitaciones por consumo de bateria que realizo los desarrolladores de google
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                getApplicationContext().startForegroundService(intent);
            }else{
                startService(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBoundService){
            myBluetoothSerialService.setMessageHandler(null);
            unbindService(serviceConnection);
            mBoundService = false;
        }

        stopService(new Intent(this, MyBluetoothSerialService.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUiToastEvent(UiToastEvent event) {
        Config.Mensaje(PanelActivity.this, event.getMessage(), event.getLongToast(), event.getIsWarning());
    };
}
