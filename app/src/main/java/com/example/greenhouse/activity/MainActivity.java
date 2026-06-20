package com.example.greenhouse.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.example.greenhouse.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MqttAsyncClient mqttClient;
    private FirebaseFirestore db;
    private DatabaseReference rtDb;

    // =========================
    // MQTT CONFIG
    // =========================
    private static final String SERVER_URI = "tcp://broker.emqx.io:1883";

    // ESP32 -> Android
    public static final String TOPIC_SOIL = "esp32/soil";
    public static final String TOPIC_LDR = "esp32/ldr";
    public static final String TOPIC_DEVICE_STATUS = "greenhouse/ben10/device/status";
    public static final String TOPIC_LAMP_STATUS = "esp32/lamp/status";
    public static final String TOPIC_PUMP_STATUS = "esp32/pump/status";
    public static final String TOPIC_LAMP_MODE = "esp32/lamp/mode";
    public static final String TOPIC_PUMP_MODE = "esp32/pump/mode";

    // Android -> ESP32
    public static final String TOPIC_LAMP_CMD = "esp32/lamp/cmd";
    public static final String TOPIC_PUMP_CMD = "esp32/pump/cmd";

    // =========================
    // LISTENER UNTUK FRAGMENT
    // =========================
    public interface OnMqttMessageListener {
        void onMessageReceived(String topic, String payload);
    }

    private final List<OnMqttMessageListener> listeners = new ArrayList<>();

    public void addMqttListener(OnMqttMessageListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeMqttListener(OnMqttMessageListener listener) {
        listeners.remove(listener);
    }

    // =========================
    // LIFECYCLE
    // =========================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigasi bottomNavigasi = new BottomNavigasi(this, binding);
        bottomNavigasi.setup();

        // 1. Initialize Cloud Firestore (Step 3 in Assistant)
        db = FirebaseFirestore.getInstance();

        // 2. Initialize Realtime Database
        rtDb = FirebaseDatabase.getInstance().getReference();

        // 3. Add Test Data (Step 4 in Assistant)
        // testFirestore();
        // testRealtimeDatabase();

        connectMQTT();
    }

    /**
     * Contoh menambahkan data ke Firestore sesuai petunjuk Assistant.
     */
    private void testFirestore() {
        // Create a new user with a first and last name
        java.util.Map<String, Object> user = new java.util.HashMap<>();
        user.put("first", "Ada");
        user.put("last", "Lovelace");
        user.put("born", 1815);

        // Add a new document with a generated ID
        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FIRESTORE_TEST", "DocumentSnapshot added with ID: " + documentReference.getId());
                    Toast.makeText(this, "Data Firestore berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w("FIRESTORE_TEST", "Error adding document", e);
                });
    }

    /**
     * Contoh menambahkan data ke Realtime Database.
     */
    private void testRealtimeDatabase() {
        String message = "Hello from GreenHouse Android!";
        rtDb.child("test_connection").setValue(message)
                .addOnSuccessListener(unused -> {
                    Log.d("RTDB_TEST", "Data berhasil dikirim ke Realtime Database");
                    Toast.makeText(this, "Realtime Database Terhubung!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("RTDB_TEST", "Gagal kirim data ke RTDB: " + e.getMessage());
                });
    }

    // =========================
    // CONNECT MQTT
    // =========================
    private void connectMQTT() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                Log.d("MQTT_STATUS", "MQTT sudah terhubung");
                return;
            }

            String userId;

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            } else {
                userId = "guest";
            }

            String clientId = "Greenhouse_Main_" + userId + "_" + System.currentTimeMillis();

            mqttClient = new MqttAsyncClient(
                    SERVER_URI,
                    clientId,
                    new MemoryPersistence()
            );

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("MQTT_STATUS", "Koneksi MQTT terputus");

                    if (cause != null) {
                        Log.e("MQTT_STATUS", "Penyebab: " + cause.getMessage());
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();

                    Log.d("MQTT_RECEIVE", "Topic: " + topic + " | Payload: " + payload);

                    runOnUiThread(() -> {
                        List<OnMqttMessageListener> copyListeners = new ArrayList<>(listeners);

                        for (OnMqttMessageListener listener : copyListeners) {
                            if (listener != null) {
                                listener.onMessageReceived(topic, payload);
                            }
                        }
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT_DELIVERY", "Pesan MQTT berhasil dikirim");
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT_STATUS", "MQTT berhasil terhubung");
                    subscribeTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT_ERROR", "MQTT gagal terhubung");

                    if (exception != null) {
                        Log.e("MQTT_ERROR", "Penyebab: " + exception.getMessage());
                    }
                }
            });

        } catch (MqttException e) {
            Log.e("MQTT_ERROR", "Exception saat connect MQTT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================
    // SUBSCRIBE TOPICS
    // =========================
    private void subscribeTopics() {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                Log.e("MQTT_SUBSCRIBE", "Gagal subscribe, MQTT belum terhubung");
                return;
            }

            mqttClient.subscribe(TOPIC_SOIL, 1);
            mqttClient.subscribe(TOPIC_LDR, 1);
            mqttClient.subscribe(TOPIC_DEVICE_STATUS, 1);
            mqttClient.subscribe(TOPIC_LAMP_STATUS, 1);
            mqttClient.subscribe(TOPIC_PUMP_STATUS, 1);
            mqttClient.subscribe(TOPIC_LAMP_MODE, 1);
            mqttClient.subscribe(TOPIC_PUMP_MODE, 1);

            Log.d("MQTT_SUBSCRIBE", "Semua topic berhasil disubscribe");

        } catch (MqttException e) {
            Log.e("MQTT_SUBSCRIBE", "Gagal subscribe topic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================
    // PUBLISH COMMAND
    // DIPANGGIL DARI HOMEFRAGMENT
    // =========================
    public void publishCommand(String topic, String command) {
        try {
            if (mqttClient == null) {
                Log.e("MQTT_PUBLISH", "mqttClient masih null");
                connectMQTT();
                return;
            }

            if (!mqttClient.isConnected()) {
                Log.e("MQTT_PUBLISH", "MQTT belum terhubung, mencoba konek ulang");
                connectMQTT();
                return;
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(command.getBytes(StandardCharsets.UTF_8));
            message.setQos(0);
            message.setRetained(false);

            mqttClient.publish(topic, message);

            Log.d("MQTT_PUBLISH", "Berhasil publish");
            Log.d("MQTT_PUBLISH", "Topic: " + topic);
            Log.d("MQTT_PUBLISH", "Command: " + command);

        } catch (MqttException e) {
            Log.e("MQTT_PUBLISH", "Gagal publish MQTT: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================
    // CEK STATUS MQTT
    // =========================
    public boolean isMqttConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    // =========================
    // LOAD FRAGMENT
    // =========================
    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_up,
                        R.anim.stay,
                        R.anim.stay,
                        R.anim.slide_down
                )
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }

    // =========================
    // ON DESTROY
    // =========================
    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                Log.d("MQTT_STATUS", "MQTT disconnected");
            }
        } catch (MqttException e) {
            Log.e("MQTT_ERROR", "Gagal disconnect MQTT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}