package com.d2magicos.carrobluetooth.util;

public interface Constants {

    long STATUS_UPDATE_INTERVAL    = 150;

    // Tipos de mensajes enviados al BluetoothService Handler
    int MESSAGE_STATE_CHANGE            = 1;
    int MESSAGE_READ                    = 2;
    int MESSAGE_WRITE                   = 3;
    int MESSAGE_DEVICE_NAME             = 4;
    int MESSAGE_TOAST                   = 5;;
    int CONNECT_DEVICE_SECURE           = 9;
    int CONNECT_DEVICE_INSECURE         = 10;

    String DEVICE_NAME                  = "device_name";
    String TOAST                        = "toast";

    int BLUETOOTH_SERVICE_NOTIFICATION_ID           = 100;

}