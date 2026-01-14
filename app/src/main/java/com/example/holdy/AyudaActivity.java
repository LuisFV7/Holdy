package com.example.holdy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;


import java.util.HashMap;
import java.util.Map;

public class AyudaActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBackNuevo;
    private MessageAdapter adapter;
    private Handler mainHandler;

    //  Llamada a Cloud Functions (backend seguro)
    private FirebaseFunctions functions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayuda);

        mainHandler = new Handler(Looper.getMainLooper());
        functions = FirebaseFunctions.getInstance("europe-west1");

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBackNuevo = findViewById(R.id.btnBackNuevo);

        adapter = new MessageAdapter();
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        // Mensaje inicial del bot
        adapter.addMessage(new Message(
                "¡Hola! Soy el asistente de Holdy. Pregúntame sobre los Sensores, la Cámara o las Notificaciones.",
                1
        ));
        rvMessages.scrollToPosition(adapter.getItemCount() - 1);

        btnSend.setOnClickListener(v -> sendMessage());
        btnBackNuevo.setOnClickListener(v -> finish());
    }

    private void sendMessage() {
        String userMessage = etMessage.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        etMessage.setText("");

        // 1) Pintar mensaje del usuario
        adapter.addMessage(new Message(userMessage, 0));
        rvMessages.scrollToPosition(adapter.getItemCount() - 1);

        // 2) Pintar "Escribiendo..."
        Message loading = new Message("Escribiendo...", 1);
        adapter.addMessage(loading);
        rvMessages.scrollToPosition(adapter.getItemCount() - 1);

        // 3) Llamar a backend (Cloud Function)
        callSupportChat(userMessage,
                reply -> mainHandler.post(() -> {
                    // Quita "Escribiendo..." (último mensaje)
                    adapter.removeLastMessage();
                    // Añade respuesta del bot
                    adapter.addMessage(new Message(reply, 1));
                    rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                }),
                e -> mainHandler.post(() -> {
                    adapter.removeLastMessage();
                    adapter.addMessage(new Message("¡Fallo LLM! " + safeError(e), 1));
                    rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                })
        );
    }

    private void callSupportChat(String userText,
                                 java.util.function.Consumer<String> onOk,
                                 java.util.function.Consumer<Exception> onErr) {

        Map<String, Object> data = new HashMap<>();
        data.put("message", userText);

        functions
                .getHttpsCallable("supportChat")
                .call(data)
                .addOnSuccessListener(result -> {
                    Object raw = result.getData();
                    if (!(raw instanceof Map)) {
                        onOk.accept("Sin respuesta del servidor.");
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> res = (Map<String, Object>) raw;

                    Object replyObj = res.get("reply");
                    String reply = (replyObj != null) ? replyObj.toString() : "Sin respuesta.";
                    onOk.accept(reply);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof Exception) onErr.accept((Exception) e);
                    else onErr.accept(new Exception(e));
                });

    }

    private String safeError(Exception e) {
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
            return "Functions " + ffe.getCode() + ": " + ffe.getMessage();
        }
        return (e.getMessage() != null) ? e.getMessage() : "Error desconocido";
    }

}
