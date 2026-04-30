package hk.hkucs.comp7506_project.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hk.hkucs.comp7506_project.R;
import hk.hkucs.comp7506_project.model.ChatSession;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionVH> {

    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
        void onSessionLongClick(ChatSession session);
    }

    private final List<ChatSession> sessions;
    private final OnSessionClickListener listener;
    private String activeSessionId = "";

    public SessionAdapter(List<ChatSession> sessions, OnSessionClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    public void setActiveSessionId(String id) {
        this.activeSessionId = id == null ? "" : id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionVH holder, int position) {
        ChatSession session = sessions.get(position);
        holder.title.setText(session.getTitle());
        holder.preview.setText(session.getPreview());

        boolean active = session.getId().equals(activeSessionId);
        holder.itemView.setAlpha(active ? 1f : 0.78f);
        holder.itemView.setBackgroundResource(
                active ? R.color.notemind_chip : android.R.color.transparent);

        holder.itemView.setOnClickListener(v -> listener.onSessionClick(session));

        holder.itemView.setOnLongClickListener(v -> {
            listener.onSessionLongClick(session);
            return true;
        });
    }

    @Override
    public int getItemCount() { return sessions.size(); }

    static class SessionVH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView preview;

        SessionVH(@NonNull View v) {
            super(v);
            title   = v.findViewById(R.id.sessionTitle);
            preview = v.findViewById(R.id.sessionPreview);
        }
    }
}
