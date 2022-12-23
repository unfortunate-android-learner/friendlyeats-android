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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.example.fireeats.adapter.MessageScrollAdapter
import com.google.firebase.example.fireeats.databinding.FragmentMessagesScrollBinding
import com.google.firebase.example.fireeats.model.Chat
import com.google.firebase.example.fireeats.model.Message
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MessagesScrollFragment: Fragment(), MessageScrollAdapter.OnScrollMessageSelectedListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var chatRef: DocumentReference
    private lateinit var query: Query
    private lateinit var query2: Query

    private lateinit var lastVisibleProduct: DocumentSnapshot
    private var isLastProductReached: Boolean = false

    private lateinit var binding: FragmentMessagesScrollBinding
    private var adapter: MessageScrollAdapter? = null
    private var isScrolling: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMessagesScrollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatId: String = MessagesFragmentArgs.fromBundle(requireArguments()).chatId
        val userId: String = MessagesFragmentArgs.fromBundle(requireArguments()).userId
        val chat: Chat = MessagesFragmentArgs.fromBundle(requireArguments()).chat

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)

        // Initialize Firestore
        firestore = Firebase.firestore

        chatRef = firestore.collection("chts").document(chatId)

        query = chatRef.collection("msgs")
            //.orderBy("timestamp", Query.Direction.ASCENDING)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            //.whereGreaterThan("timestamp", Timestamp.now())
            .whereLessThan("timestamp", Timestamp.now())
            .limit(7)

        chat.members?.let { members ->
            query2 = firestore.collection("usrs")
                .whereIn(FieldPath.documentId(), members)

            query2.get().addOnSuccessListener { document ->
                val userSnapshotList = document.documents

                // RecyclerView
                query.let {
                    adapter = object : MessageScrollAdapter(it, userSnapshotList, this@MessagesScrollFragment, true) {
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
                    binding.rvMessages.adapter = adapter
                }

                val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvMessages.layoutManager = layoutManager

                // Start listening for Firestore updates
                adapter?.startListening()
            }.addOnFailureListener { exception ->
                Log.d("MessagesFragment", exception.message.toString())
            }
        }

        initRecyclerViewOnScrollListener()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()

        adapter?.stopListening()
    }

    private fun initRecyclerViewOnScrollListener() {
        val onScrollListener: RecyclerView.OnScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                        isScrolling = true
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                    if (layoutManager != null) {
                        val reachedTop = layoutManager.findFirstCompletelyVisibleItemPosition()

                        if (isScrolling && reachedTop == 0) {
                            isScrolling = false

                            if(!isLastProductReached) {
                                query = chatRef.collection("msgs")
                                    //.orderBy("timestamp", Query.Direction.ASCENDING)
                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                    .startAfter(lastVisibleProduct)
                                    .limit(7)

                                adapter?.setNewQuery(query)
                            }
                        }
                    }
                }
            }
        binding.rvMessages.addOnScrollListener(onScrollListener)
    }

    override fun onScrollMessageSelected(message: Message) {

    }

    override fun setLastVisibleProduct(lastVisibleProduct: DocumentSnapshot) {
        this.lastVisibleProduct = lastVisibleProduct
    }

    override fun setLastProductReached(isLastProductReached: Boolean) {
        this.isLastProductReached = isLastProductReached
    }

    override fun scrollToBottom() {
        adapter?.let { it ->
            binding.rvMessages.scrollToPosition(it.itemCount - 1)
        }
    }
}