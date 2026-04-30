package hk.hkucs.comp7506_project.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hk.hkucs.comp7506_project.R;
import hk.hkucs.comp7506_project.model.ChatMessage;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnMessageLongClickListener {
        void onMessageLongClick(String content);
    }

    private final List<ChatMessage> messages;
    private final OnMessageLongClickListener longClickListener;

    public MessageAdapter(List<ChatMessage> messages, OnMessageLongClickListener longClickListener) {
        this.messages = messages;
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == ChatMessage.TYPE_USER) {
            return new UserVH(inf.inflate(R.layout.item_message_user, parent, false));
        }
        return new AiVH(inf.inflate(R.layout.item_message_ai, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String content = messages.get(position).getContent();
        TextView bubble;
        if (holder instanceof UserVH) {
            bubble = ((UserVH) holder).text;
        } else {
            bubble = ((AiVH) holder).text;
        }
        bubble.setText(content);
        bubble.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onMessageLongClick(content);
            return true;
        });
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class UserVH extends RecyclerView.ViewHolder {
        final TextView text;
        UserVH(@NonNull View v) {
            super(v);
            text = v.findViewById(R.id.messageText);
        }
    }

    static class AiVH extends RecyclerView.ViewHolder {
        final TextView text;
        AiVH(@NonNull View v) {
            super(v);
            text = v.findViewById(R.id.messageText);
        }
    }
}
