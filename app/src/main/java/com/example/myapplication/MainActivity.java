package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.example.myapplication.MqttPublisher;

import java.util.Properties;

public class MainActivity extends AppCompatActivity {
    MqttPublisher client = null;
    MqttClient mqttClient;

    String topic = "PDR/xyz";
    String brokerURI = "tcp://150.82.177.245:1883";
    //String brokerURI = "ssl://150.82.177.120:8883;    //ssl/tls接続
    /*
     * Android Studioでは [localhost], [127.0.0.1]はエミュレーターや電話回線に設定されており
     * 通信を行う為にはホストやIPを指定する必要がある。
     */
    //String trustStore   = this.getClass().getResource("***/crt.jks").getPath();
    String clientID = "PDR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new MqttPublisher(brokerURI, clientID);
        client.connect();
    }


    public void sendMessage(View view) {
        if (!client.connection_status) {return;}

        int id = 1;
        double x = 2.5; double y = 3.5; double z = 4.5; double yaw = 3.14/2;
        client.publish(topic, id, x, y, z, yaw);
    }


    public void reConnect(View view) {
        if (client.connection_status) {return;}

        client.connect();
    }

    public void disConnect(View view) {
        if (!client.connection_status) {return;}

        client.disconnect();
    }
}