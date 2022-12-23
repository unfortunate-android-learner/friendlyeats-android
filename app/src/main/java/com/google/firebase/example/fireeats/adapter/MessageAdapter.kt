package com.google.firebase.example.fireeats.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.example.fireeats.databinding.ItemMessageBinding
import com.google.firebase.example.fireeats.databinding.ItemUserBinding
import com.google.firebase.example.fireeats.model.Message
import com.google.firebase.example.fireeats.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

open class MessageAdapter(query: Query, val userSnapshotList: List<DocumentSnapshot>, private val listener: OnMessageSelectedListener) :
    FirestoreAdapter<MessageAdapter.ViewHolder>(query) {

    interface OnMessageSelectedListener {

        fun onMessageSelected(message: Message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMessageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false),
            userSnapshotList)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(val binding: ItemMessageBinding, val userSnapshotList: List<DocumentSnapshot>) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            snapshot: DocumentSnapshot,
            listener: OnMessageSelectedListener?
        ) {
            val message = snapshot.toObject<Message>()
            if (message == null) {
                return
            }

            binding.tvTitle.text = message.message

            for(user in userSnapshotList) {
                if(user.id == message.sender) {
                    val user = user.toObject<User>()
                    user?.let {
                        binding.tvTitle.text = user.name + ": " + binding.tvTitle.text.toString()
                    }
                }
            }

            // Click listener
            binding.root.setOnClickListener {
                listener?.onMessageSelected(message)
            }
        }
    }
}