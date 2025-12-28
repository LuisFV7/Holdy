package com.example.holdy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    // Coordenadas destino (buzón)
    private final LatLng buzon = new LatLng(38.996651, -0.166009);
    private final String tituloBuzon = "Buzón Holdy";

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String> requestLocationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    activarMiUbicacion();
                    centrarEnMiUbicacion();
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    // Aun así mostramos el destino
                    if (mMap != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(buzon, 16f));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        MaterialButton btnDirections = findViewById(R.id.btnDirections);
        btnDirections.setOnClickListener(v -> abrirNavegacionGoogleMaps(buzon));

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Marcador del destino
        mMap.addMarker(new MarkerOptions().position(buzon).title(tituloBuzon));

        // Pedir permiso y mostrar ubicación
        comprobarPermisoUbicacion();

        // Si aún no hay permiso, al menos enfocamos el destino
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(buzon, 16f));
    }

    private void comprobarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            activarMiUbicacion();
            centrarEnMiUbicacion();
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void activarMiUbicacion() {
        if (mMap == null) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    private void centrarEnMiUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && mMap != null) {
                        LatLng yo = new LatLng(location.getLatitude(), location.getLongitude());
                        // Enfoca tu ubicación (y dejas el destino marcado)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(yo, 15f));
                    } else {
                        // Si no hay lastLocation (a veces pasa), mantenemos destino
                        if (mMap != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(buzon, 16f));
                    }
                });
    }

    private void abrirNavegacionGoogleMaps(LatLng destino) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destino.latitude + "," + destino.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + destino.latitude + "," + destino.longitude);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }
}
