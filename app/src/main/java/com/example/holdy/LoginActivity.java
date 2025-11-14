package com.example.holdy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    // Campos para mostrar/ocultar contraseña
    private EditText etContrasena;
    private ImageButton btnEye;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        // --- Firebase Auth ---
        mAuth = FirebaseAuth.getInstance();

        // --- Google Sign-In config ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // requiere SHA-1/256 configurados en Firebase
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- Ajustar contenido a barras del sistema ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Iniciar sesión con Google ---
        findViewById(R.id.btnGoogle).setOnClickListener(v -> signInWithGoogle());

        // --- Mostrar/Ocultar contraseña 👁 ---
        etContrasena = findViewById(R.id.etContrasena);
        btnEye = findViewById(R.id.btnEye);

        btnEye.setOnClickListener(v -> {
            if (passwordVisible) {
                // Ocultar contraseña
                etContrasena.setTransformationMethod(
                        new android.text.method.PasswordTransformationMethod());
                btnEye.setImageResource(R.drawable.mdi_eye);      // ojo cerrado
            } else {
                // Mostrar contraseña
                etContrasena.setTransformationMethod(
                        new android.text.method.HideReturnsTransformationMethod());
                btnEye.setImageResource(R.drawable.mdi_eye);  // ojo abierto
            }
            etContrasena.setSelection(etContrasena.getText().length()); // mantener cursor al final
            passwordVisible = !passwordVisible;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Si ya hay usuario logueado, saltamos login
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goHome();
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account == null) {
                    Toast.makeText(this, "No se pudo obtener la cuenta de Google", Toast.LENGTH_SHORT).show();
                    return;
                }
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed. code=" + e.getStatusCode(), e);
                Toast.makeText(this, "Error al iniciar con Google (" + e.getStatusCode() + ")", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        String idToken = account.getIdToken();
        if (idToken == null) {
            Toast.makeText(this, "Falta ID Token (revisa SHA-1/256 en Firebase)", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "firebaseAuthWithGoogle: " + account.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, (Task<AuthResult> task) -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String nombre = (user != null && user.getDisplayName() != null)
                                ? user.getDisplayName() : "usuario";
                        Toast.makeText(this, "¡Bienvenido, " + nombre + "!", Toast.LENGTH_SHORT).show();
                        goHome();
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Firebase signInWithCredential failed", e);
                        Toast.makeText(this, "Error al iniciar sesión con Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void volverAMain(View view) {
        finish();
    }

    public void irARegistrarte(View view) {
        Intent i = new Intent(this, RegistrarteActivity.class);
        startActivity(i);
    }
}
