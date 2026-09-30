package com.example.evidenciascesba;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FotoAdapter extends RecyclerView.Adapter<FotoAdapter.FotoViewHolder> {

    private final List<FotoEvidencia> listaFotos;

    public FotoAdapter(List<FotoEvidencia> listaFotos) {
        this.listaFotos = listaFotos;
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_foto, parent, false);
        return new FotoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        FotoEvidencia foto = listaFotos.get(position);
        holder.imgMiniatura.setImageURI(foto.getUri());
        holder.txtHoraMiniatura.setText(foto.getHoraCaptura());
    }

    @Override
    public int getItemCount() {
        return listaFotos.size();
    }

    static class FotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMiniatura;
        TextView txtHoraMiniatura;

        FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMiniatura = itemView.findViewById(R.id.imgMiniatura);
            txtHoraMiniatura = itemView.findViewById(R.id.txtHoraMiniatura);
        }
    }
}