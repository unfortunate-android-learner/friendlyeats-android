package com.google.firebase.example.fireeats

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.example.fireeats.adapter.MessageTalkAdapter
import com.google.firebase.example.fireeats.databinding.FragmentMessagesTalkBinding
import com.google.firebase.example.fireeats.model.Chat
import com.google.firebase.example.fireeats.model.Message
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class MessagesTalkFragment: Fragment(), MessageTalkAdapter.OnScrollMessageSelectedListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var chatDocument: DocumentReference
    private lateinit var query: Query
    private lateinit var query2: Query

    private lateinit var lastVisibleProduct: DocumentSnapshot
    private var isLastProductReached: Boolean = false

    private lateinit var binding: FragmentMessagesTalkBinding
    private var adapter: MessageTalkAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMessagesTalkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatId: String = MessagesTalkFragmentArgs.fromBundle(requireArguments()).chatId
        val userId: String = MessagesTalkFragmentArgs.fromBundle(requireArguments()).userId
        val chat: Chat = MessagesTalkFragmentArgs.fromBundle(requireArguments()).chat

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)

        // Initialize Firestore
        firestore = Firebase.firestore

        chatDocument = firestore.collection("chts").document(chatId)

        query = chatDocument.collection("msgs")
            //.orderBy("timestamp", Query.Direction.ASCENDING)
            .orderBy("time", Query.Direction.DESCENDING)
            //.whereGreaterThan("timestamp", Timestamp.now())
            //.whereLessThan("timestamp", Timestamp.now())
            .limit(7)

        chat.chat_members?.let { members ->
            query2 = firestore.collection("usrs")
                //.whereIn(FieldPath.documentId(), members)

            query2.get().addOnSuccessListener { document ->
                val userSnapshotList = document.documents

                // RecyclerView
                query.let {
                    adapter = object : MessageTalkAdapter(it, userSnapshotList, this@MessagesTalkFragment, true, System.currentTimeMillis()) {
                        override fun onDataChanged() {
                            // we don't do that here
                        }

                        override fun onError(e: FirebaseFirestoreException) {
                            // Show a snackbar on errors
                            Snackbar.make(
                                binding.root,
                                "Error: check logs for info.", Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                    binding.rvChats.adapter = adapter
                }

                val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvChats.layoutManager = layoutManager

                // Start listening for Firestore updates
                adapter?.startListening()
            }.addOnFailureListener { exception ->
                Log.d("MessagesFragment", exception.message.toString())
            }
        }

        initRecyclerViewOnScrollListener()

        binding.btnSend.setOnClickListener {
            addMessage(chatDocument, Message(
                message = binding.etMessage.text.toString(),
                sender = userId,
                time = System.currentTimeMillis().toString()
            )
            )

            binding.etMessage.setText("")
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()

        adapter?.stopListening()
    }

    private fun addMessage(chatRef: DocumentReference, message: Message): Task<Void> {
        // Create reference for new rating, for use inside the transaction
        val messageRef = chatRef.collection("msgs").document()

        // In a transaction, add the new rating and update the aggregate totals
        return firestore.runTransaction { transaction ->
            val chat = transaction.get(chatRef).toObject<Chat>()
                ?: throw Exception("Restaurant not found at ${chatRef.path}")

            // Commit to Firestore
            transaction.set(chatRef, chat)
            transaction.set(messageRef, message)

            null
        }
    }

    private fun initRecyclerViewOnScrollListener() {
        val onScrollListener: RecyclerView.OnScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                    if (layoutManager != null) {
                        val reachedTop = layoutManager.findFirstCompletelyVisibleItemPosition()

                        if (reachedTop == 0) {
                            if(!isLastProductReached) {
                                query = chatDocument.collection("msgs")
                                    //.orderBy("timestamp", Query.Direction.ASCENDING)
                                    .orderBy("time", Query.Direction.DESCENDING)
                                    .startAfter(lastVisibleProduct)
                                    .limit(7)

                                adapter?.setNewQuery(query)
                            }
                        }
                    }
                }
            }
        binding.rvChats.addOnScrollListener(onScrollListener)
    }

    override fun onMessageClicked(message: Message) {

    }

    override fun onMessageDeleted(snapshot: DocumentSnapshot) {
        val messageDocument = chatDocument.collection("msgs").document(snapshot.id)

        val updatedData = hashMapOf(
            "deleted" to true
        )

        messageDocument.set(updatedData, SetOptions.merge())
    }

    override fun setLastVisibleProduct(lastVisibleProduct: DocumentSnapshot) {
        this.lastVisibleProduct = lastVisibleProduct
    }

    override fun setLastProductReached(isLastProductReached: Boolean) {
        this.isLastProductReached = isLastProductReached
    }

    override fun scrollToBottom() {
        adapter?.let { it ->
            binding.rvChats.scrollToPosition(it.itemCount - 1)
        }
    }
}