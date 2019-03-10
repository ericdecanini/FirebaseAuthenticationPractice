package com.ericdecanini.authenticationpractice

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.FacebookCallback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.TwitterCore


class MainActivity : AppCompatActivity() {

    val TAG = MainActivity::class.java.simpleName
    val auth = FirebaseAuth.getInstance()
    val facebookCallbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initFacebookLogin()
        initTwitterSignIn()
        initGoogleSignIn()
        signOutButton.setOnClickListener { auth.signOut() }
    }

    private fun initFacebookLogin() {
        facebookLogInButton.setReadPermissions("email", "public_profile")
        // If you are using in a fragment, call loginButton.setFragment(this);

        // Callback registration
        facebookLogInButton.setReadPermissions()
        facebookLogInButton.registerCallback(facebookCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {}
            override fun onError(exception: FacebookException) {}
        })

    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        updateUI(user!!)
    }

    private fun updateUI(user: FirebaseUser) {
        // Blank method aye lmao
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = auth.currentUser
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }

                }
    }

    private fun initTwitterSignIn() {
        twitterLogInButton.callback = object : Callback<TwitterSession>() {
            override fun success(result: Result<TwitterSession>) {
                // Do something with result, which provides a TwitterSession for making API calls
                val session = TwitterCore.getInstance().sessionManager.activeSession
                handleTwitterLogin(session)
            }

            override fun failure(exception: TwitterException) {
                // Do something on failure
            }
        }
    }

    private fun handleTwitterLogin(session: TwitterSession) {
        val credential = TwitterAuthProvider.getCredential(
                session.authToken.token,
                session.authToken.secret)

        auth.signInWithCredential(credential)
    }

    private fun initGoogleSignIn() {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build()

        // Build a GoogleSignInClient with the options specified by gso.
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInButton.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        twitterLogInButton.onActivityResult(requestCode, resultCode, data)
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 0) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
            auth.signInWithCredential(credential)
        }
    }
}
