package com.google.firebase.example.fireeats.adapter

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.example.fireeats.databinding.ItemMessageBinding
import com.google.firebase.example.fireeats.model.Message
import com.google.firebase.example.fireeats.model.User
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

open class MessageTalkAdapter(var query: Query, val userSnapshotList: List<DocumentSnapshot>, private val listener: OnScrollMessageSelectedListener,
                                var isFirstTime: Boolean = false, var currentTimeMilli: Long) :
    RecyclerView.Adapter<MessageTalkAdapter.ViewHolder>(), EventListener<QuerySnapshot> {

    private var registration: ListenerRegistration? = null

    private var docSnapshotList = ArrayList<DocumentSnapshot>()

    interface OnScrollMessageSelectedListener {

        fun onMessageDeleted(snapshot: DocumentSnapshot)

        fun onMessageClicked(message: Message)

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

    class ViewHolder(val binding: ItemMessageBinding, val userSnapshotList: List<DocumentSnapshot>) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            snapshot: DocumentSnapshot,
            listener: OnScrollMessageSelectedListener?
        ) {
            val message = snapshot.toObject<Message>()
            if (message == null) {
                return
            }

            if(message.deleted == true) {
                binding.layoutMessage.visibility = View.GONE
            } else {
                binding.layoutMessage.visibility = View.VISIBLE
            }

            binding.tvTitle.text = message.message

            for(user in userSnapshotList) {
                if(user.id == message.sender) {
                    val user = user.toObject<User>()
                    user?.let {
                        binding.tvTitle.text = user.name + ": " + binding.tvTitle.text.toString() +
                                "\n" + SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(Date(message.time!!.toLong()))
                    }
                }
            }

            binding.root.setOnClickListener {
                listener?.onMessageClicked(message)
            }

            binding.ibDelete.setOnClickListener {
                listener?.onMessageDeleted(snapshot)
            }
        }
    }

    fun setNewQuery(query: Query) {
        this.query = query

        registration = this.query.addSnapshotListener(this)
    }

    fun startListening() {
        if (registration == null) {
            registration = query.addSnapshotListener(this)
        }
    }

    // Add this method
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onEvent(documentSnapshots: QuerySnapshot?, e: FirebaseFirestoreException?) {
        // Handle errors
        if (e != null) {
            Log.w("MessageTalkAdapter", "onEvent:error", e)
            return
        }

        // Dispatch the event
        if (documentSnapshots != null) {
            for (change in documentSnapshots.documentChanges) {
                // snapshot of the changed document
                when (change.type) {
                    DocumentChange.Type.ADDED -> {
                        val currentDateTime = Date(currentTimeMilli)
                        val messageDateTime = Date(change.document.toObject<Message>().time!!.toLong())

                        if(messageDateTime.after(currentDateTime)) {
                            onDocumentNewAdded(change)
                        } else {
                            onDocumentAdded(change)
                        }
                    }
                    DocumentChange.Type.MODIFIED -> {
                        onDocumentModified(change)
                    }
                    DocumentChange.Type.REMOVED -> {
                        // DocumentChange.Type.REMOVED
                        // It is running every time document is added
                        // So, I just commented it
                        // and I will use
                        // DocumentChange.Type.MODIFIED
                        // for deleting
                        //onDocumentRemoved(change)
                    }
                }
            }
        }

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

    private fun onDocumentAdded(change: DocumentChange) {
        docSnapshotList.add(0, change.document)
        notifyItemInserted(0)
    }

    private fun onDocumentNewAdded(change: DocumentChange) {
        docSnapshotList.add(docSnapshotList.size, change.document)
        notifyItemInserted(docSnapshotList.size)
    }

    private fun onDocumentModified(change: DocumentChange) {
        val data = change.document

        for(item in docSnapshotList) {
            if(item.id == data.id) {
                val foundItem = item

                val index = docSnapshotList.indexOf(foundItem)

                val mutableList = docSnapshotList.toMutableList()

                mutableList.removeAt(index)
                mutableList.add(index, data)

                docSnapshotList = mutableList.toList() as ArrayList<DocumentSnapshot>
                notifyDataSetChanged()
            }
        }
    }

    private fun onDocumentRemoved(change: DocumentChange) {
        val data = change.document

        for(item in docSnapshotList) {
            if(item.id == data.id) {
                val foundItem = item

                val index = docSnapshotList.indexOf(foundItem)

                val mutableList = docSnapshotList.toMutableList()

                mutableList.removeAt(index)

                docSnapshotList = mutableList.toList() as ArrayList<DocumentSnapshot>
                notifyDataSetChanged()
            }
        }
    }

    fun stopListening() {
        registration?.remove()
        registration = null

        docSnapshotList.clear()
        notifyDataSetChanged()
    }

    open fun onError(e: FirebaseFirestoreException) {
        Log.w("MessageTalkAdapter", "onError", e)
    }

    open fun onDataChanged() {}

    override fun getItemCount(): Int {
        return docSnapshotList.size
    }

    protected fun getSnapshot(index: Int): DocumentSnapshot {
        return docSnapshotList[index]
    }
}