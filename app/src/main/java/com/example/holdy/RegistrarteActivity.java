package com.example.holdy;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistrarteActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    private EditText etNombre, etApellidos, etCorreo, etPass, etPassConfirm, etTelefono;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registrarte);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        etNombre       = findViewById(R.id.etNombre);
        etApellidos    = findViewById(R.id.etApellidos);
        etCorreo       = findViewById(R.id.etCorreo);
        etPass         = findViewById(R.id.etPass);
        etPassConfirm  = findViewById(R.id.etPassConfirm);
        etTelefono     = findViewById(R.id.etTelefono);

        findViewById(R.id.btnContinuar).setOnClickListener(v -> register());

        // LINK "Ya tengo cuenta"
        TextView tvLogin = findViewById(R.id.tvLoginLink);
        SpannableString content = new SpannableString(tvLogin.getText().toString());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        tvLogin.setText(content);

        tvLogin.setOnClickListener(v -> {
            Intent i = new Intent(RegistrarteActivity.this, LoginActivity.class);
            startActivity(i);
        });

        // GOOGLE SIGN-IN CONFIG
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btnGoogleRegister).setOnClickListener(v -> signInWithGoogle());
    }

    public void volverAMain(android.view.View view) {
        finish();
    }

    // REGISTRO NORMAL ------------------------------------
    private void register() {
        String nombre   = val(etNombre);
        String apellidos= val(etApellidos);
        String correo   = val(etCorreo);
        String pass1    = val(etPass);
        String pass2    = val(etPassConfirm);
        String telefono = val(etTelefono);

        boolean hasError = false;

        if (nombre.isEmpty()) { etNombre.setError("Nombre requerido"); hasError = true; }
        if (apellidos.isEmpty()) { etApellidos.setError("Apellidos requeridos"); hasError = true; }

        if (TextUtils.isEmpty(correo) || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Correo inválido"); hasError = true;
        }

        if (pass1.isEmpty()) {
            etPass.setError("Contraseña requerida"); hasError = true;
        } else if (pass1.length() < 6) {
            etPass.setError("Mín. 6 caracteres"); hasError = true;
        }

        if (pass2.isEmpty()) {
            etPassConfirm.setError("Confirmar contraseña requerida"); hasError = true;
        } else if (!pass1.equals(pass2)) {
            etPassConfirm.setError("Las contraseñas no coinciden"); hasError = true;
        }

        if (telefono.isEmpty()) {
            etTelefono.setError("Teléfono requerido"); hasError = true;
        } else if (!telefono.matches("^[0-9+()\\-\\s]{7,}$")) {
            etTelefono.setError("Teléfono inválido"); hasError = true;
        }

        if (hasError) return;

        auth.createUserWithEmailAndPassword(correo, pass1)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                task.getException() != null ? task.getException().getLocalizedMessage() : "Error de registro",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    String display = (nombre + " " + apellidos).trim();
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(display)
                            .build());

                    Map<String, Object> perfil = new HashMap<>();
                    perfil.put("nombre", nombre);
                    perfil.put("apellidos", apellidos);
                    perfil.put("correo", correo);
                    perfil.put("telefono", telefono);
                    perfil.put("creadoEn", System.currentTimeMillis());

                    db.collection("usuarios").document(user.getUid())
                            .set(perfil)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this,
                                        "Registro completado correctamente",
                                        Toast.LENGTH_LONG).show();

                                // ANTES: iba a LoginActivity
                                // Intent i = new Intent(RegistrarteActivity.this, LoginActivity.class);

                                // AHORA: IR DIRECTO A INICIO
                                Intent i = new Intent(RegistrarteActivity.this, InicioActivity.class);
                                startActivity(i);
                                finish(); // ← cerrar pantalla de registro
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "No se pudo guardar el perfil: " + e.getLocalizedMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                });
    }

    private String val(EditText e) { return e.getText().toString().trim(); }

    // GOOGLE SIGN-IN -------------------------------------
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        //USUARIO YA AUTENTICADO
                        FirebaseUser user = auth.getCurrentUser();

                        Toast.makeText(this,
                                "¡Bienvenido, " + (user != null ? user.getDisplayName() : "") + "!",
                                Toast.LENGTH_SHORT).show();

                        //IR DIRECTO A INICIO DESPUÉS DE GOOGLE
                        Intent intent = new Intent(RegistrarteActivity.this, InicioActivity.class);
                        startActivity(intent);
                        finish(); // cerrar activity

                    } else {
                        Toast.makeText(this,
                                "Error al iniciar sesión con Firebase",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}