package com.example.holdy;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Usuarios {

    public static void guardarUsuario(final FirebaseUser user) {
        if (user == null) return;

        Usuario u = new Usuario(
                user.getDisplayName() != null ? user.getDisplayName() : "",
                user.getEmail() != null ? user.getEmail() : ""
        );

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(user.getUid())
                .set(u)
                .addOnSuccessListener(unused ->
                        System.out.println("Usuario guardado correctamente"))
                .addOnFailureListener(e ->
                        System.err.println("Error al guardar usuario: " + e.getLocalizedMessage()));
    }
}
