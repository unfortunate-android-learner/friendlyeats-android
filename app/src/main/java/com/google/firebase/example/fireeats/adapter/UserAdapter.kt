package com.google.firebase.example.fireeats.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.example.fireeats.databinding.ItemUserBinding
import com.google.firebase.example.fireeats.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

open class UserAdapter(query: Query, private val listener: OnUserSelectedListener) :
    FirestoreAdapter<UserAdapter.ViewHolder>(query) {

    interface OnUserSelectedListener {

        fun onUserSelected(userSnapshot: DocumentSnapshot)
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
            listener: OnUserSelectedListener?
        ) {

            val user = snapshot.toObject<User>()
            if (user == null) {
                return
            }

            binding.tvTitle.text = user.name

            // Click listener
            binding.root.setOnClickListener {
                listener?.onUserSelected(snapshot)
            }
        }
    }
}