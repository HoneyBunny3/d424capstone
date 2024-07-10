package com.example.d424capstone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.d424capstone.R;
import com.example.d424capstone.entities.ContactMessage;

import java.util.ArrayList;
import java.util.List;

public class ContactMessageAdapter extends RecyclerView.Adapter<ContactMessageAdapter.ContactMessageViewHolder> {

    private List<ContactMessage> contactMessages;
    private LayoutInflater inflater;

    public ContactMessageAdapter(Context context, List<ContactMessage> contactMessages) {
        this.inflater = LayoutInflater.from(context);
        this.contactMessages = contactMessages != null ? contactMessages : new ArrayList<>();
    }

    @NonNull
    @Override
    public ContactMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.contact_message_item, parent, false);
        return new ContactMessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactMessageViewHolder holder, int position) {
        ContactMessage currentMessage = contactMessages.get(position);
        holder.firstNameTextView.setText(currentMessage.getFirstName());
        holder.lastNameTextView.setText(currentMessage.getLastName());
        holder.emailTextView.setText(currentMessage.getEmail());
        holder.subjectTextView.setText(currentMessage.getSubject());
        holder.messageTextView.setText(currentMessage.getMessage());
        holder.timestampTextView.setText(currentMessage.getTimestamp().toString());
    }

    @Override
    public int getItemCount() {
        return contactMessages.size();
    }

    public static class ContactMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView firstNameTextView;
        private TextView lastNameTextView;
        private TextView emailTextView;
        private TextView subjectTextView;
        private TextView messageTextView;
        private TextView timestampTextView;

        public ContactMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            firstNameTextView = itemView.findViewById(R.id.firstNameTextView);
            lastNameTextView = itemView.findViewById(R.id.lastNameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            subjectTextView = itemView.findViewById(R.id.subjectTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}