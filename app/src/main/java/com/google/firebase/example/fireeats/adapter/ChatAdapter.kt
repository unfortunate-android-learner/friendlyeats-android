package com.google.firebase.example.fireeats.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.example.fireeats.databinding.ItemUserBinding
import com.google.firebase.example.fireeats.model.Chat
import com.google.firebase.example.fireeats.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

open class ChatAdapter(query: Query, private val listener: OnChatSelectedListener) :
    FirestoreAdapter<ChatAdapter.ViewHolder>(query) {

    interface OnChatSelectedListener {

        fun onChatSelected(chatSnapshot: DocumentSnapshot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemUserBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            snapshot: DocumentSnapshot,
            listener: OnChatSelectedListener?
        ) {

            val chat = snapshot.toObject<Chat>()
            if (chat == null) {
                return
            }

            binding.tvTitle.text = chat.name

            // Click listener
            binding.root.setOnClickListener {
                listener?.onChatSelected(snapshot)
            }
        }
    }
}