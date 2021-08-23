package com.d2magicos.carrobluetooth.service;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.d2magicos.carrobluetooth.R;
import com.d2magicos.carrobluetooth.event.UiToastEvent;
import com.d2magicos.carrobluetooth.helper.NotificationHelper;
import com.d2magicos.carrobluetooth.util.Constants;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public class MyBluetoothSerialService extends Service {

    private static final String TAG = MyBluetoothSerialService.class.getSimpleName();
    public static final String KEY_MAC_ADDRESS = "KEY_MAC_ADDRESS";

    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter mAdapter; // Adaptador de nuestro bluetooth

    //Begin Hilos
    private Handler mHandlerActivity; // Hilo para comunicar el estado de la conexion al hilo principal de activity
    private ConnectThread mConnectThread; // Hilo para intentar conectarse
    private ConnectedThread mConnectedThread; // Hilo cuando ya se conecto
    //End Hilos

    private int mState;
    private int mNewState;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private final IBinder mBinder = new MySerialServiceBinder();

    private long statusUpdatePoolInterval = Constants.STATUS_UPDATE_INTERVAL;



    @Override
    public void onCreate() {
        super.onCreate();
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mState = STATE_NONE;
        mNewState = mState;

        if (mAdapter == null) {

            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_bluetooth_adapter_error), true, true));
            stopSelf(); //Detener el servicio
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                startForeground(Constants.BLUETOOTH_SERVICE_NOTIFICATION_ID, this.getNotification(null));
            }
        }
    }

    private Notification getNotification(String message){

        if(message == null) message = getString(R.string.text_bluetooth_service_foreground_message);

        return new NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SERVICE_ID)
                .setContentTitle(getString(R.string.text_bluetooth_service))
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.start();

        if (intent != null) {
            String deviceAddress = intent.getStringExtra(KEY_MAC_ADDRESS);
            if (deviceAddress != null) {
                try {
                    BluetoothDevice device = mAdapter.getRemoteDevice(deviceAddress.toUpperCase());
                    this.connect(device, true); // Conexion al modulo bluetooth
                } catch (IllegalArgumentException e) {
                    EventBus.getDefault().post(new UiToastEvent(e.getMessage(), true, true));
                    disconnectService(); // Desconecta el servicio
                    stopSelf(); // Detiene por completo el servicio
                }
            }
        } else {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_unknown_error), true, true));
            disconnectService(); // Desconecta el servicio
            stopSelf(); // Detiene por completo el servicio
        }

        return Service.START_NOT_STICKY; //Le indica al servicio cuando se sale de la app se cierre el servicio
    }

    synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Update UI title
        updateUserInterfaceTitle();
    }

    synchronized void connect(BluetoothDevice device, boolean secure) {

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    public void disconnectService() {
        this.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public synchronized int getState() {
        return mState;
    }

    public class MySerialServiceBinder extends Binder {
        public MyBluetoothSerialService getService() {
            return MyBluetoothSerialService.this;
        }
    }

    public void setMessageHandler(Handler myServiceMessageHandler) {
        this.mHandlerActivity = myServiceMessageHandler;
    }


    //Crear una conexion como cliente
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Cancelamos siempre la busqueda de dispositivos para evitar que relentize la conexion actual
            mAdapter.cancelDiscovery();

            // Realizamos la conexion al BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect(); //Conexion establecida con el blFuetooth
            } catch (IOException | NullPointerException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException | NullPointerException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +" socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            //Reiniciamos el Thread porque hemos terminado con la conexion
            synchronized (this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Enviamos el nombre del dispositivo al cual nos hemos conectado a nuestro activity
        if(mHandlerActivity != null){
            Message msg = mHandlerActivity.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            mHandlerActivity.sendMessage(msg);
        }
        updateUserInterfaceTitle();

        try {
            wait(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Este hilo se ejecuta durante una conexión con un dispositivo remoto.
     * Maneja todas las transmisiones entrantes y salientes.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            // Colocar el codigo para escuchar cuando ingresan datos nuevos (InputStream)
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer); //Enviamos el dato al arduino
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        updateUserInterfaceTitle();
    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        if(mHandlerActivity != null){
            Message msg = mHandlerActivity.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, getString(R.string.text_unable_to_connect_to_device));
            msg.setData(bundle);
            mHandlerActivity.sendMessage(msg);
        }

        mState = STATE_NONE;
        updateUserInterfaceTitle();

        this.start();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        if(mHandlerActivity != null){
            Message msg = mHandlerActivity.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, getString(R.string.text_device_connection_was_lost));
            msg.setData(bundle);
            mHandlerActivity.sendMessage(msg);
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        this.start();
    }

    private void serialWriteBytes(byte[] b) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(b);
    }

    public void serialWriteString(String s){
        byte buffer[] = s.getBytes();
        this.serialWriteBytes(buffer);
        Log.d("send_data: ", "caracter enviado " + s);
    }

    public void serialWriteByte(byte b){
        byte[] c = {b};
        serialWriteBytes(c);
    }

    /**
     * Actualizamos el estado global del servicio para actualizar el titulo del activity
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        mNewState = mState;
        //Si el hilo del activity sigue activo le enviamos el estado del Servicio actual
        if(mHandlerActivity != null){
            mHandlerActivity.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
        }
    }

    public long getStatusUpdatePoolInterval(){
        return this.statusUpdatePoolInterval;
    }

    public void setStatusUpdatePoolInterval(long poolInterval){
        this.statusUpdatePoolInterval = poolInterval;
    }

}
