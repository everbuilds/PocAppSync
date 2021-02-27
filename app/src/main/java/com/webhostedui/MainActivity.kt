package com.webhostedui

import android.R.attr.*
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobile.client.AWSMobileClient
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.model.temporal.Temporal.Timestamp.*
import com.amplifyframework.datastore.generated.model.GameRoom
import com.amplifyframework.datastore.generated.model.Player
import java.time.temporal.Temporal


class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.button).setOnClickListener {
            Log.i("idk", AWSMobileClient.getInstance().tokens.idToken.tokenString)
            val options: RestOptions = RestOptions.builder()
                .addHeader("Authorization", AWSMobileClient.getInstance().tokens.idToken.tokenString)
                .addPath("/testapi")
                .build()

            val gameroom: GameRoom = GameRoom.builder().build()
            //gameroom.players (Player.builder().name("alberto").score(10).build())
            Amplify.API.mutate(
                ModelMutation.create(gameroom),
                { response ->
                    Log.i("MyAmplifyApp", "Added GameRoom with id: " + response.getData().getId())
                    val player = Player.builder()
                        .name("alberto")
                        .score(10)
                        .gameroom(gameroom)
                        .lastinteraction(now())
                        .build()
                    Amplify.API.mutate(
                        ModelMutation.create(player),
                        {
                            responsePlayer -> Log.i(
                                "MyAmplifyApp",
                                "Added GameRoom with id: " + responsePlayer.getData().getId()
                            )
                        },
                        { error: ApiException? -> Log.e("MyAmplifyApp", "Create failed", error) }
                    )
                },
                { error: ApiException? -> Log.e("MyAmplifyApp", "Create failed", error) }
            )
        }
        findViewById<Button>(R.id.play).setOnClickListener {
            Amplify.Auth.fetchAuthSession({ el ->
                if (el.isSignedIn) {
                    val type = arrayOf("Single-Player", "Multi-Player")
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Modalità di gioco")
                    builder.setItems(type) { _, index ->
                        Log.i("scelta", type[index])
                    }
                    this.runOnUiThread {
                        builder.show()
                    }
                } else {
                    login()
                }
            }, {})
        }
        findViewById<Button>(R.id.registrazione).setOnClickListener{
            register()
        }
        findViewById<Button>(R.id.login).setOnClickListener{ login() }
        findViewById<Button>(R.id.logout).setOnClickListener{ logout() }
        findViewById<Button>(R.id.settings).setOnClickListener{
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        updateUI();
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AWSCognitoAuthPlugin.WEB_UI_SIGN_IN_ACTIVITY_CODE) {
            Amplify.Auth.handleWebUISignInResponse(data)
        }
        updateUI();
    }
    private fun login(){
        Amplify.Auth.signInWithWebUI(
                this,
                { result ->
                    toast("Accesso effettuato correttamente")
                    updateUI()
                },
                { error -> Log.e("Error", error.toString()) }
        )

    }
    private fun register(){
        Amplify.Auth.signInWithWebUI(
                this,
                { result ->
                    toast("Accesso effettuato correttamente")
                    updateUI()
                },
                { error -> Log.e("Error", error.toString()) }
        )

    }
    private fun logout(){
        Amplify.Auth.signOut({
            toast("Logout effettuato correttamente")
            updateUI()
        },
                { error -> Log.e("Error", error.toString()) }
        )

    }

    private fun toast(text: String, size: Int = Toast.LENGTH_SHORT){
        this.runOnUiThread{
            val toast = Toast.makeText(this, text, size)
            toast.show()
        }
    }
    private fun updateUI(){
        this.runOnUiThread {
            try {
                if(Amplify.Auth.currentUser != null) {
                    findViewById<Button>(R.id.login).visibility = View.GONE
                    findViewById<Button>(R.id.registrazione).visibility = View.GONE
                    findViewById<Button>(R.id.logout).visibility = View.VISIBLE
                } else {
                    findViewById<Button>(R.id.logout).visibility = View.GONE
                    findViewById<Button>(R.id.login).visibility = View.VISIBLE
                    findViewById<Button>(R.id.registrazione).visibility = View.VISIBLE
                }
            } catch (ex: Exception) {
                findViewById<Button>(R.id.logout).visibility = View.GONE
                findViewById<Button>(R.id.login).visibility = View.VISIBLE
                findViewById<Button>(R.id.registrazione).visibility = View.VISIBLE

            }
        }
    }
}