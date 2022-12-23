package com.google.firebase.example.fireeats.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.example.fireeats.databinding.ItemMessageBinding
import com.google.firebase.example.fireeats.databinding.ItemUserBinding
import com.google.firebase.example.fireeats.model.Message
import com.google.firebase.example.fireeats.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject

open class MessageScrollAdapter(query: Query, val userSnapshotList: List<DocumentSnapshot>, private val listener: OnScrollMessageSelectedListener,
var isFirstTime: Boolean = false) :
    FirestoreReverseAdapter<MessageScrollAdapter.ViewHolder>(query) {

    interface OnScrollMessageSelectedListener {

        fun onScrollMessageSelected(message: Message)

        fun setLastVisibleProduct(lastVisibleProduct: DocumentSnapshot)

        fun setLastProductReached(isLastProductReached: Boolean)

        fun scrollToBottom()
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

    override fun onEvent(documentSnapshots: QuerySnapshot?, e: FirebaseFirestoreException?) {
        super.onEvent(documentSnapshots, e)

        if(documentSnapshots != null) {
            val querySnapshotSize = documentSnapshots.size()

            if(isFirstTime) {
                listener.scrollToBottom()

                isFirstTime = false
            }

            if(querySnapshotSize < 7) {
                listener.setLastProductReached(true)
            } else {
                val lastVisibleProduct = documentSnapshots.documents[querySnapshotSize - 1]
                listener.setLastVisibleProduct(lastVisibleProduct)
            }
        }
    }

    class ViewHolder(val binding: ItemMessageBinding, val userSnapshotList: List<DocumentSnapshot>) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            snapshot: DocumentSnapshot,
            listener: OnScrollMessageSelectedListener?
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
                        binding.tvTitle.text = user.name + ": " + binding.tvTitle.text.toString() +
                                "\n" + message.timestamp?.toDate()
                    }
                }
            }

            // Click listener
            binding.root.setOnClickListener {
                listener?.onScrollMessageSelected(message)
            }
        }
    }
}