package com.google.firebase.example.fireeats

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.example.fireeats.adapter.MessageAdapter
import com.google.firebase.example.fireeats.databinding.FragmentMessagesBinding
import com.google.firebase.example.fireeats.model.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject

class MessagesFragment: Fragment(), MessageAdapter.OnMessageSelectedListener {

    lateinit var firestore: FirebaseFirestore
    private var query: Query? = null
    private var query2: Query? = null

    private lateinit var chatRef: DocumentReference

    private lateinit var binding: FragmentMessagesBinding

    private lateinit var currentTimestamp: String

    private var adapter: MessageAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentTimestamp = System.currentTimeMillis().toString()

        val chatId: String = MessagesFragmentArgs.fromBundle(requireArguments()).chatId
        val userId: String = MessagesFragmentArgs.fromBundle(requireArguments()).userId
        val chat: Chat = MessagesFragmentArgs.fromBundle(requireArguments()).chat

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)

        // Initialize Firestore
        firestore = Firebase.firestore

        // Get reference to the restaurant
        chatRef = firestore.collection("chts").document(chatId)

        // Get the 50 highest rated restaurants
        query = chatRef.collection("msgs")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .whereGreaterThan("timestamp", Timestamp.now())

        chat.members?.let { members ->
            query2 = firestore.collection("usrs")
                .whereIn(FieldPath.documentId(), members)

            query2!!.get().addOnSuccessListener { document ->
                val userSnapshotList = document.documents

                // RecyclerView
                query?.let {
                    adapter = object : MessageAdapter(it, userSnapshotList, this@MessagesFragment) {
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

                binding.rvChats.layoutManager = LinearLayoutManager(context)

                // Start listening for Firestore updates
                adapter?.startListening()
            }.addOnFailureListener { exception ->
                Log.d("MessagesFragment", exception.message.toString())
            }
        }

        binding.btnSend.setOnClickListener {
            addMessage(chatRef, Message(
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

    override fun onMessageSelected(message: Message) {

    }
}