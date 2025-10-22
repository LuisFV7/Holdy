package com.example.holdy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001; // Código de solicitud para el inicio de sesión
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;  // FirebaseAuth para gestionar la autenticación

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configura Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Usa el Client ID del archivo strings.xml
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configura el botón de inicio de sesión de Google
        findViewById(R.id.googleBoton).setOnClickListener(view -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Resultado de Google Sign-In
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // Google Sign-In fue exitoso, ahora autenticamos con Firebase
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("Google Sign-In", "Error de inicio de sesión con Google", e);
                Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d("Google Sign-In", "firebaseAuthWithGoogle:" + account.getId());

        // Usa el token de ID de Google para autenticar con Firebase
        Task<AuthResult> authResultTask = mAuth.signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null));
        authResultTask.addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // El inicio de sesión con Firebase fue exitoso
                FirebaseUser user = mAuth.getCurrentUser();
                Toast.makeText(this, "¡Bienvenido, " + user.getDisplayName() + "!", Toast.LENGTH_SHORT).show();
                // Puedes agregar más lógica aquí, como redirigir a otra actividad
            } else {
                // Si la autenticación falla, muestra un mensaje de error
                Toast.makeText(this, "Error al iniciar sesión con Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void lanzarAcercaDe(View view){
        Intent i = new Intent(this, AcercaDeActivity.class);
        startActivity(i);
    }
}
