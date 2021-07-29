package com.example.myapplication;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;    //いわゆるRetain機能
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;

import java.util.Date;
import java.util.Properties;


public class MqttPublisher {
    private MqttClient mqttClient;
    private String brokerURI;    // e.g. "tcp://150.82.177.221:1883"  (localhostはNG)
    public String clientID;
    public boolean connection_status = false;

    public int seq = 0;
    public int secs = 0;
    public BigDecimal nsecs = new BigDecimal("0.0");

    public class Quaternion {
        double x;
        double y;
        double z;
        double w;
    }


    public MqttPublisher(String brokerURI, String clientID) {
        /**
         * コンストラクタ
         */
        this.brokerURI = brokerURI;
        this.clientID = clientID;
    }


    public boolean connect() {
        /**
         * 接続 (デフォルト)
         *
         * 戻り値 : true/接続成功　false/接続失敗
         */
        try {
            // memory persistence の設定が必須
            mqttClient = new MqttClient(brokerURI, clientID, new MemoryPersistence());

            mqttClient.connect();

        } catch (MqttException e) {
            String emsg = String.format("\n==========\n Connection failure\n %s \n==========", e.getCause());
            Log.e("connect", emsg);
        }

        /* 接続状態の確認 */
        if (mqttClient.isConnected()) {
            String dmsg = String.format("\n==========\n Connection success\n==========");
            Log.d("connect", dmsg);
            connection_status = true;
        } else { connection_status = false; }

        return connection_status;
    }


    public boolean connect(String trustStore) {
        /**
         * 接続　(ssl/tls)
         */
        try {
            // memory persistence の設定が必須
            mqttClient = new MqttClient(brokerURI, clientID, new MemoryPersistence());

            MqttConnectOptions ops = new MqttConnectOptions();
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            Properties prop = new Properties();
            prop.setProperty("com.ibm.ssl.trustStore", trustStore);
            connOpts.setSSLProperties(prop);
            mqttClient.connect(connOpts);

        } catch (MqttException e) {
            System.out.println("==========");
            System.out.println("Connection failure");
            e.printStackTrace();
            System.out.println("==========");
        }

        /* 接続状態の確認 */
        if (mqttClient.isConnected()) {
            System.out.println("==========");
            System.out.println("Connecting");
            System.out.println("==========");
            connection_status = true;
        } else { connection_status = false; }

        return connection_status;
    }


    public void publish(String topic, int id, double x, double y, double z, double yaw) {
        /**
         * publish
         *
         * 計測データをJSON形式でPublishする
         */
        double uts = System.currentTimeMillis()/1000.0;
        MqttMessage msg = new MqttMessage(makeMessage(uts, id, x, y, z, yaw));

        // msg.setQos(1);    //QosレベルはPub側/Sub側双方で設定されより低い方へ適用される。 i.e.Sub側も変更しないと変わらない

        try {

            mqttClient.publish(topic, msg);

        } catch (MqttException e) {
            String emsg = String.format("\n==========\n Transmission failure\n %s \n==========", e.getCause());
            Log.e("publish", emsg);
        }

    }


    private byte[] makeMessage(double uts ,int id, double x, double y, double z, double yaw) {
        /**
         * ペイロードの作成
         *
         * 計測データをJSON形式化/バイト配列化して返す
         */
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(uts));
        System.out.println(bigDecimal);
        int intValue = bigDecimal.intValue();
        secs = intValue;
        nsecs = bigDecimal.subtract(new BigDecimal(intValue)).multiply(new BigDecimal(1000000000.0));

        ArrayList<ArrayList<Double>> RM = new ArrayList<ArrayList<Double>>();
        ArrayList<Double> tmp0 = new ArrayList<Double>();
        ArrayList<Double> tmp1 = new ArrayList<Double>();
        ArrayList<Double> tmp2 = new ArrayList<Double>();

        tmp0.add(Math.cos(yaw)); tmp0.add(-Math.sin(yaw)); tmp0.add(0.0);
        RM.add(tmp0);
        tmp1.add(Math.sin(yaw)); tmp1.add(Math.cos(yaw)); tmp1.add(0.0);
        RM.add(tmp1);
        tmp2.add(0.0); tmp2.add(0.0); tmp2.add(1.0);
        RM.add(tmp2);

        Quaternion quaternion = DCM(RM);

        /* JSON形式へ変換 */
        JSONObject json = new JSONObject();
        JSONObject header = new JSONObject();
        JSONObject stamp = new JSONObject();
        JSONObject pose = new JSONObject();
        JSONObject position = new JSONObject();
        JSONObject orientation = new JSONObject();

        try {
            header.put("seq", seq);
            stamp.put("secs", secs);
            stamp.put("nsecs", nsecs);
            header.put("stamp", stamp);
            header.put("frame_id", 2);

            json.put("header", header);

            position.put("x", x); position.put("y", y); position.put("z", z);
            //position.put("yaw", yaw);
            pose.put("position", position);
            orientation.put("x", quaternion.x); orientation.put("y", quaternion.y);
            orientation.put("z", quaternion.z); orientation.put("w", quaternion.w);
            pose.put("orientation", orientation);
            json.put("pose", pose);
        } catch (JSONException e) {
            String emsg = String.format("\n==========\n Json format error\n %s \n==========", e.getCause());
            Log.e("makeMessage", emsg);
        }

        seq++;

        String msg = json.toString();
        return msg.getBytes();
    }


    private Quaternion DCM(ArrayList<ArrayList<Double>> matrix) {
        Quaternion res = new Quaternion();
        double SUM = 1.0 + matrix.get(0).get(0) + matrix.get(1).get(1) + matrix.get(2).get(2);

        res.w = Math.sqrt(SUM)/2.0;
        res.x = -(matrix.get(2).get(1) - matrix.get(1).get(2)) / (4.0 * SUM);
        res.y = -(matrix.get(0).get(2) - matrix.get(2).get(0)) / (4.0 * SUM);
        res.z = -(matrix.get(1).get(0) - matrix.get(0).get(1)) / (4.0 * SUM);

        System.out.println(matrix);


        return res;
    }


    public void disconnect() {
        /**
         * 切断
         */
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            String emsg = String.format("\n==========\n Disconnection error\n %s \n==========", e.getCause());
            Log.e("disconnect", emsg);
        }

        /* 接続状態の確認 */
        if (mqttClient.isConnected()) {
            String dmsg = String.format("\n==========\n Connection success\n==========");
            Log.d("connect", dmsg);
            connection_status = true;
        } else { connection_status = false; }
    }
}
