package com.google.firebase.example.fireeats.adapter

import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import com.google.firebase.firestore.*
import java.util.ArrayList

/**
 * RecyclerView adapter for displaying the results of a Firestore [Query].
 *
 * Note that this class forgoes some efficiency to gain simplicity. For example, the result of
 * [DocumentSnapshot.toObject] is not cached so the same object may be deserialized
 * many times as the user scrolls.
 */
abstract class FirestoreReverseAdapter<VH : RecyclerView.ViewHolder>(private var query: Query) :
    RecyclerView.Adapter<VH>(), EventListener<QuerySnapshot> {

    private var registration: ListenerRegistration? = null

    private val snapshots = ArrayList<DocumentSnapshot>()

    fun setNewQuery(query: Query) {
        this.query = query

        registration = this.query.addSnapshotListener(this)
    }

    fun startListening() {
        // TODO(developer): Implement
        if (registration == null) {
            registration = query.addSnapshotListener(this)
        }
    }

    // Add this method
    override fun onEvent(documentSnapshots: QuerySnapshot?, e: FirebaseFirestoreException?) {

        // Handle errors
        if (e != null) {
            Log.w(TAG, "onEvent:error", e)
            return
        }

        // Dispatch the event
        if (documentSnapshots != null) {
            for (change in documentSnapshots.documentChanges) {
                // snapshot of the changed document
                when (change.type) {
                    DocumentChange.Type.ADDED -> {
                        // TODO: handle document added
                        onDocumentAdded(change)
                    }
                    DocumentChange.Type.MODIFIED -> {
                        // TODO: handle document changed
                        onDocumentModified(change)
                    }
                    DocumentChange.Type.REMOVED -> {
                        // TODO: handle document removed
                        onDocumentRemoved(change)
                    }
                }
            }
        }

        onDataChanged()
    }

    private fun onDocumentAdded(change: DocumentChange) {
        //snapshots.add(change.newIndex, change.document)
        snapshots.add(0, change.document)
        notifyItemInserted(change.newIndex)
    }

    private fun onDocumentModified(change: DocumentChange) {
        if (change.oldIndex == change.newIndex) {
            // Item changed but remained in same position
            snapshots[change.oldIndex] = change.document
            notifyItemChanged(change.oldIndex)
        } else {
            // Item changed and changed position
            snapshots.removeAt(change.oldIndex)
            snapshots.add(change.newIndex, change.document)
            notifyItemMoved(change.oldIndex, change.newIndex)
        }
    }

    private fun onDocumentRemoved(change: DocumentChange) {
        snapshots.removeAt(change.oldIndex)
        notifyItemRemoved(change.oldIndex)
    }

    fun stopListening() {
        registration?.remove()
        registration = null

        snapshots.clear()
        notifyDataSetChanged()
    }

    fun setQuery(query: Query) {
        // Stop listening
        stopListening()

        // Clear existing data
        snapshots.clear()
        notifyDataSetChanged()

        // Listen to new query
        this.query = query
        startListening()
    }

    open fun onError(e: FirebaseFirestoreException) {
        Log.w(TAG, "onError", e)
    }

    open fun onDataChanged() {}

    override fun getItemCount(): Int {
        return snapshots.size
    }

    protected fun getSnapshot(index: Int): DocumentSnapshot {
        return snapshots[index]
    }

    companion object {

        private const val TAG = "FirestoreAdapter"
    }
}
