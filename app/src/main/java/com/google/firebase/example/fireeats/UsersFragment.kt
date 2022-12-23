package com.google.firebase.example.fireeats

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.example.fireeats.adapter.UserAdapter
import com.google.firebase.example.fireeats.databinding.FragmentUsersBinding
import com.google.firebase.example.fireeats.model.User
import com.google.firebase.example.fireeats.viewmodel.MainActivityViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class UsersFragment : Fragment(), UserAdapter.OnUserSelectedListener {

    lateinit var firestore: FirebaseFirestore
    private var query: Query? = null

    private lateinit var binding: FragmentUsersBinding

    private var adapter: UserAdapter? = null

    private lateinit var viewModel: MainActivityViewModel

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result -> this.onSignInResult(result) }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        viewModel.isSigningIn = false

        if (result.resultCode != Activity.RESULT_OK) {
            if (response == null) {
                // User pressed the back button.
                requireActivity().finish()
            } else if (response.error != null && response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                showSignInErrorDialog(R.string.message_no_network)
            } else {
                showSignInErrorDialog(R.string.message_unknown)
            }
        }
    }

    private fun showSignInErrorDialog(@StringRes message: Int) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_sign_in_error)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.option_retry) { _, _ -> startSignIn() }
            .setNegativeButton(R.string.option_exit) { _, _ -> requireActivity().finish() }.create()

        dialog.show()
    }

    private fun startSignIn() {
        // Sign in with FirebaseUI
        val intent = AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(listOf(AuthUI.IdpConfig.EmailBuilder().build()))
            .setIsSmartLockEnabled(false)
            .build()

        signInLauncher.launch(intent)
        viewModel.isSigningIn = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View model
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true)

        // Firestore
        firestore = Firebase.firestore

        //createUsers()

        // Get the 50 highest rated restaurants
        query = firestore.collection("usrs")
            .orderBy("name", Query.Direction.DESCENDING)
            .limit(10)

        // RecyclerView
        query?.let {
            adapter = object : UserAdapter(it, this@UsersFragment) {
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
            binding.rvUsers.adapter = adapter
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(context)
    }

    private fun shouldStartSignIn(): Boolean {
        return !viewModel.isSigningIn && Firebase.auth.currentUser == null
    }

    override fun onStart() {
        super.onStart()

        if (shouldStartSignIn()) {
            startSignIn()
            return
        }

        // Start listening for Firestore updates
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onUserSelected(user: DocumentSnapshot) {
        // Go to the details page for the selected restaurant
        val userData = user.toObject<User>()

        if(userData == null) {
            return
        }

        val action = UsersFragmentDirections
            .actionUsersFragmentToChatsFragment(
                userData,
                user.id
            )

        findNavController().navigate(action)
    }

    private fun createUsers() {
        val userRef = firestore.collection("usrs")

        val user1 = User("Luke", emptyList())
        val user2 = User("Vader", emptyList())
        val user3 = User("Yoda", emptyList())

        // Add restaurant
        userRef.add(user1)
        userRef.add(user2)
        userRef.add(user3)
    }
}