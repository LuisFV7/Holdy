package com.example.holdy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.net.ConnectException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AyudaActivity extends AppCompatActivity {


    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBackNuevo;
    private MessageAdapter adapter;
    private Handler mainHandler;

    private static final String BASE_URL = "https://api.openai.com";
    private static final String CHAT_URL = BASE_URL + "/v1/chat/completions";


    //private static final String API_KEY


    private static final String PROJECT_ID = "PEGAR_TU_PROJ_ID_AQUÍ"; 

    private static final String MODEL = "gpt-3.5-turbo";

    private final OkHttpClient http = new OkHttpClient();
    private final List<JSONObject> history = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... (El resto del onCreate es el mismo) ...
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayuda);

        mainHandler = new Handler(Looper.getMainLooper());

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBackNuevo = findViewById(R.id.btnBackNuevo);

        adapter = new MessageAdapter();
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        history.add(msg("system",
                "Eres un ayudante servicial y experto en el sistema Holdy de buzones inteligentes, sensores y configuración IoT. Responde breve, claro y con pasos."));

        adapter.addMessage(new Message(
                "¡Hola! Soy el asistente de Holdy. Pregúntame sobre los Sensores, la Cámara o las Notificaciones.",
                1
        ));
        rvMessages.scrollToPosition(adapter.getItemCount() - 1);

        btnSend.setOnClickListener(v -> sendMessage());
        btnBackNuevo.setOnClickListener(v -> finish());
    }

    private void sendMessage() {
        // ... (El resto de sendMessage es el mismo) ...
        String userMessage = etMessage.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        etMessage.setText("");

        adapter.addMessage(new Message(userMessage, 0));
        rvMessages.scrollToPosition(adapter.getItemCount() - 1);

        Message loading = new Message("Escribiendo...", 1);
        adapter.addMessage(loading);
        rvMessages.scrollToPosition(adapter.getItemCount() - 1);

        history.add(msg("user", userMessage));

        new Thread(() -> {
            String answer;
            try {
                answer = callChat();
                history.add(msg("assistant", answer));
            } catch (Exception e) {
                // ... (El diagnóstico del error es el mismo) ...
                String errorMessage = e.getMessage();

                if (e.getCause() instanceof ConnectException) {
                    errorMessage = "Error de Red: No se pudo conectar al servidor de la IA. Revisa tu conexión a Internet.";
                } else if (errorMessage != null && errorMessage.contains("HTTP Error 401")) {
                    errorMessage = " 401: CLAVE API INCORRECTA.";
                } else if (errorMessage != null && errorMessage.contains("HTTP Error 404")) {
                    errorMessage = " 404: MODELO INCORRECTO. Revisa: " + MODEL;
                } else if (errorMessage != null && errorMessage.contains("HTTP Error 400")) {
                    errorMessage = " 400: Error de Petición. Confirma que el PROJECT ID y la Clave son correctos.";
                } else {
                    errorMessage = "Error desconocido: " + errorMessage;
                }

                answer = "¡Fallo LLM! " + errorMessage;
            }

            String finalAnswer = answer;
            mainHandler.post(() -> {
                adapter.removeLastMessage();
                adapter.addMessage(new Message(finalAnswer, 1));
                rvMessages.scrollToPosition(adapter.getItemCount() - 1);
            });
        }).start();
    }

    private String callChat() throws Exception {
        JSONArray messages = new JSONArray();
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) messages.put(history.get(i));

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("model", MODEL);
        bodyJson.put("messages", messages);
        bodyJson.put("stream", false);

        RequestBody body = RequestBody.create(
                bodyJson.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        // *** CAMBIO CLAVE: AÑADIR EL ENCABEZADO DEL PROJECT ID ***
        Request request = new Request.Builder()
                .url(CHAT_URL)
                //.addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("OpenAI-Project", PROJECT_ID) // <-- ¡Esta es la corrección!
                .post(body)
                .build();

        try (Response resp = http.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new Exception("HTTP Error " + resp.code() + " - " +
                        (resp.body() != null ? resp.body().string() : ""));
            }

            String json = resp.body() != null ? resp.body().string() : "{}";
            JSONObject root = new JSONObject(json);

            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return "No hay respuesta del modelo.";

            JSONObject choice0 = choices.getJSONObject(0);
            JSONObject msg = choice0.getJSONObject("message");
            return msg.optString("content", "Sin contenido.");
        }
    }

    private JSONObject msg(String role, String content) {
        JSONObject o = new JSONObject();
        try {
            o.put("role", role);
            o.put("content", content);
        } catch (Exception ignored) {}
        return o;
    }
}