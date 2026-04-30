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
import hk.hkucs.comp7506_project.model.Lecture;

public class LectureAdapter extends RecyclerView.Adapter<LectureAdapter.LectureVH> {

    public interface OnLectureClickListener {
        void onLectureClick(Lecture lecture);
    }

    private final List<Lecture> items;
    private final OnLectureClickListener listener;

    public LectureAdapter(List<Lecture> items, OnLectureClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LectureVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lecture, parent, false);
        return new LectureVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LectureVH vh, int position) {
        Lecture l = items.get(position);
        vh.badge.setText("L" + l.getLectureNumber());

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF007AFF);
        vh.badge.setBackground(circle);

        vh.title.setText(l.getTitle());

        StringBuilder meta = new StringBuilder();
        if (!l.getLectureDate().isEmpty()) {
            meta.append(l.getLectureDate());
        }
        int n = l.getNoteCount();
        if (n > 0) {
            if (meta.length() > 0) meta.append("  ·  ");
            meta.append(n).append(n == 1 ? " note" : " notes");
        }
        if (meta.length() == 0) meta.append("No notes yet");
        vh.meta.setText(meta.toString());

        vh.itemView.setOnClickListener(v -> listener.onLectureClick(l));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class LectureVH extends RecyclerView.ViewHolder {
        final TextView badge, title, meta;

        LectureVH(View v) {
            super(v);
            badge = v.findViewById(R.id.lectureNumberBadge);
            title = v.findViewById(R.id.lectureTitle);
            meta  = v.findViewById(R.id.lectureMeta);
        }
    }
}
