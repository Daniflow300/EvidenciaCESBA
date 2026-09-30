package com.example.evidenciascesba;

import android.net.Uri;

public class FotoEvidencia {

    private final Uri uri;
    private final String horaCaptura;

    public FotoEvidencia(Uri uri, String horaCaptura) {
        this.uri = uri;
        this.horaCaptura = horaCaptura;
    }

    public Uri getUri() {
        return uri;
    }

    public String getHoraCaptura() {
        return horaCaptura;
    }
}