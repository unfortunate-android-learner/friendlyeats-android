package com.google.firebase.example.fireeats

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.example.fireeats.adapter.ChatAdapter
import com.google.firebase.example.fireeats.databinding.FragmentChatsBinding
import com.google.firebase.example.fireeats.model.Chat
import com.google.firebase.example.fireeats.model.User
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class ChatsFragment : Fragment(), ChatAdapter.OnChatSelectedListener {

    lateinit var firestore: FirebaseFirestore
    private var query: Query? = null

    private lateinit var user: User
    private lateinit var userId: String
    private var adapter: ChatAdapter? = null

    private lateinit var binding: FragmentChatsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        user = ChatsFragmentArgs.fromBundle(requireArguments()).user
        userId = ChatsFragmentArgs.fromBundle(requireArguments()).userId

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)

        // Initialize Firestore
        firestore = Firebase.firestore

        //createChats()

        // Get the 50 highest rated restaurants
        user.chats?.let { chats ->
            if(chats.size > 0) {
                query = firestore.collection("chts")
                    .orderBy("name", Query.Direction.DESCENDING)
                    .whereIn(FieldPath.documentId(), user.chats?: emptyList())
                    .limit(10)

                // RecyclerView
                query?.let {
                    adapter = object : ChatAdapter(it, this@ChatsFragment) {
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
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Start listening for Firestore updates
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()

        adapter?.stopListening()
    }

    private fun createChats() {
        val chatRef = firestore.collection("chts")

        val chat1 = Chat("Luke & Vader", emptyList())
        val chat2 = Chat("Luke & Yoda", emptyList())

        // Add restaurant
        chatRef.add(chat1)
        chatRef.add(chat2)
    }

    override fun onChatSelected(chat: DocumentSnapshot) {
        // Go to the details page for the selected restaurant
        val chatData = chat.toObject<Chat>()

        if(chatData == null) {
            return
        }

        val action = ChatsFragmentDirections
            .actionChatsFragmentToMessagesTalkFragment(
                userId,
                chat.id,
                chatData
            )

        findNavController().navigate(action)
    }
}