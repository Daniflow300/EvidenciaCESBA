package com.example.evidenciascesba;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView imgFoto;
    private EditText edtTitulo;
    private EditText edtDescripcion;
    private Button btnTomarFoto;
    private Button btnTomarOtraFoto;
    private Button btnGuardar;
    private Button btnCompartir;
    private TextView txtEstado;
    private TextView txtContador;
    private RecyclerView rvHistorial;

    private Uri currentPhotoUri = null;
    private int contadorFotos = 0;

    private final List<FotoEvidencia> historial = new ArrayList<>();
    private FotoAdapter fotoAdapter;

    private final ActivityResultLauncher<Intent> launcherCamara =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {

                        if (resultado.getResultCode() == RESULT_OK && currentPhotoUri != null) {

                            String horaCaptura = new SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                    .format(new Date());

                            estamparHoraEnFoto(currentPhotoUri, horaCaptura);

                            imgFoto.setImageURI(null);
                            imgFoto.setImageURI(currentPhotoUri);

                            historial.add(0, new FotoEvidencia(currentPhotoUri, horaCaptura));
                            fotoAdapter.notifyItemInserted(0);
                            rvHistorial.scrollToPosition(0);

                            contadorFotos++;
                            txtContador.setText("Fotografías capturadas: " + contadorFotos);

                            txtEstado.setText("Fotografía tomada correctamente (" + horaCaptura + ")");
                            Toast.makeText(MainActivity.this, "Fotografía recibida", Toast.LENGTH_SHORT).show();

                        } else {
                            txtEstado.setText("La fotografía fue cancelada");
                            Toast.makeText(MainActivity.this, "Operación cancelada", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private final ActivityResultLauncher<String> launcherPermisoStorage =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    guardarEnGaleria();
                } else {
                    Toast.makeText(this, "Permiso de almacenamiento necesario para guardar en galería", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgFoto = findViewById(R.id.imgFoto);
        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnTomarOtraFoto = findViewById(R.id.btnTomarOtraFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCompartir = findViewById(R.id.btnCompartir);
        txtEstado = findViewById(R.id.txtEstado);
        txtContador = findViewById(R.id.txtContador);
        rvHistorial = findViewById(R.id.rvHistorial);

        fotoAdapter = new FotoAdapter(historial);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvHistorial.setAdapter(fotoAdapter);

        btnTomarFoto.setOnClickListener(v -> abrirCamara());
        btnTomarOtraFoto.setOnClickListener(v -> abrirCamara());

        btnCompartir.setOnClickListener(v -> compartirFoto());

        btnGuardar.setOnClickListener(v -> {
            if (currentPhotoUri == null) {
                Toast.makeText(this, "Primero toma una fotografía", Toast.LENGTH_SHORT).show();
                return;
            }
            solicitarGuardadoEnGaleria();
        });
    }

    private void abrirCamara() {
        File archivo;
        try {
            archivo = crearArchivoImagen();
        } catch (IOException e) {
            Toast.makeText(this, "No se pudo crear el archivo de imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        currentPhotoUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                archivo
        );

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        launcherCamara.launch(intent);
    }

    private File crearArchivoImagen() throws IOException {
        File carpeta = new File(getCacheDir(), "fotos_evidencia");
        if (!carpeta.exists()) carpeta.mkdirs();

        String nombre = "EVID_" + System.currentTimeMillis() + ".jpg";
        return new File(carpeta, nombre);
    }

    private void estamparHoraEnFoto(Uri uri, String hora) {
        try {
            String path = getRutaDesdeUri(uri);
            if (path == null) return;

            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = 2;
            Bitmap original = BitmapFactory.decodeFile(path, opciones);
            if (original == null) return;

            Bitmap mutable = original.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutable);

            Paint paintTexto = new Paint();
            paintTexto.setColor(Color.WHITE);
            paintTexto.setTextSize(mutable.getWidth() * 0.045f);
            paintTexto.setAntiAlias(true);
            paintTexto.setShadowLayer(6f, 2f, 2f, Color.BLACK);

            float margen = mutable.getWidth() * 0.03f;
            canvas.drawText(hora, margen, mutable.getHeight() - margen, paintTexto);

            try (FileOutputStream fos = new FileOutputStream(path)) {
                mutable.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRutaDesdeUri(Uri uri) {
        String nombreArchivo = uri.getLastPathSegment();
        if (nombreArchivo == null) return null;
        File carpeta = new File(getCacheDir(), "fotos_evidencia");
        File archivo = new File(carpeta, nombreArchivo.substring(nombreArchivo.lastIndexOf('/') + 1));
        return archivo.exists() ? archivo.getAbsolutePath() : null;
    }

    private void compartirFoto() {
        if (currentPhotoUri == null) {
            Toast.makeText(this, "Primero toma una fotografía", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, currentPhotoUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Compartir evidencia"));
    }

    private void solicitarGuardadoEnGaleria() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                launcherPermisoStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
        }
        guardarEnGaleria();
    }

    private void guardarEnGaleria() {
        String path = getRutaDesdeUri(currentPhotoUri);
        if (path == null) {
            Toast.makeText(this, "No se encontró la fotografía", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap == null) {
            Toast.makeText(this, "No se pudo leer la fotografía", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Images.Media.DISPLAY_NAME, "EvidenciaCESBA_" + System.currentTimeMillis() + ".jpg");
        valores.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            valores.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EvidenciasCESBA");
        }

        Uri destino = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores);
        if (destino == null) {
            Toast.makeText(this, "No se pudo guardar en galería", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream os = getContentResolver().openOutputStream(destino)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os);
            Toast.makeText(this, "Evidencia guardada en la galería", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}