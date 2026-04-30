package hk.hkucs.comp7506_project.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hk.hkucs.comp7506_project.R;
import hk.hkucs.comp7506_project.model.NoteRecord;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteVH> {

    public interface OnNoteClickListener {
        void onNoteClick(NoteRecord note);
    }

    private final List<NoteRecord> notes;
    private final OnNoteClickListener listener;

    public NoteAdapter(List<NoteRecord> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteVH vh, int position) {
        NoteRecord note = notes.get(position);
        vh.titleView.setText(note.getTitle());

        String dateStr = note.getCreatedAt();
        if (dateStr.length() > 10) dateStr = dateStr.substring(0, 10);
        vh.dateView.setText(note.getSourceLabel() + "  ·  " + dateStr);

        vh.sourceCircle.setText(note.getSourceInitial());
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(sourceColor(note.getSourceType()));
        vh.sourceCircle.setBackground(circle);

        vh.itemView.setOnClickListener(v -> listener.onNoteClick(note));
    }

    @Override
    public int getItemCount() { return notes.size(); }

    private int sourceColor(String type) {
        switch (type) {
            case "recording": return 0xFF007AFF; // blue
            case "ocr":       return 0xFF30D158; // green
            case "ppt":       return 0xFFFF9500; // orange
            default:          return 0xFF8E8E93; // gray
        }
    }

    static class NoteVH extends RecyclerView.ViewHolder {
        final TextView sourceCircle;
        final TextView titleView;
        final TextView dateView;

        NoteVH(View v) {
            super(v);
            sourceCircle = v.findViewById(R.id.noteSourceCircle);
            titleView    = v.findViewById(R.id.noteTitle);
            dateView     = v.findViewById(R.id.noteDate);
        }
    }
}
