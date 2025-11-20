package com.example.holdy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private EditText etUsuario, etContrasena;
    private ImageButton btnEye;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Ajuste de barras
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Campos
        etUsuario = findViewById(R.id.etCorreoLogin);
        etContrasena = findViewById(R.id.etContrasena);
        btnEye = findViewById(R.id.btnEye);

        // --- MOSTRAR / OCULTAR CONTRASEÑA ---
        btnEye.setOnClickListener(v -> {
            if (passwordVisible) {
                etContrasena.setTransformationMethod(new android.text.method.PasswordTransformationMethod());
            } else {
                etContrasena.setTransformationMethod(new android.text.method.HideReturnsTransformationMethod());
            }
            etContrasena.setSelection(etContrasena.getText().length());
            passwordVisible = !passwordVisible;
        });

        // --- LOGIN CON USUARIO + PASSWORD ---
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etUsuario.getText().toString().trim();
            String pass = etContrasena.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            irAInicio(email, pass);
        });

        // Botón Google
        findViewById(R.id.btnGoogle).setOnClickListener(v -> signInWithGoogle());


        //  Olvidar contraseña → RecuperarEmailActivity
        Button btnContrasena = findViewById(R.id.btnContrasena);
        btnContrasena.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RecuperarEmailActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null);
    }

    // -----------------------------------------
    // LOGIN EMAIL + CONTRASEÑA
    // -----------------------------------------
    private void irAInicio(String email, String password) {
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, InicioActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                    }

                });
    }

    // -----------------------------------------
    // GOOGLE LOGIN
    // -----------------------------------------
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

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();
                        String nombre = (user != null && user.getDisplayName() != null)
                                ? user.getDisplayName()
                                : "usuario";

                        Toast.makeText(this, "¡Bienvenido, " + nombre + "!", Toast.LENGTH_SHORT).show();
                        //goHome();

                    } else {
                        Toast.makeText(this, "Error al iniciar sesión con Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void volverAMain(View view) {
        finish();
    }

    public void irARegistrarte(View view) {
        startActivity(new Intent(this, RegistrarteActivity.class));
    }
}