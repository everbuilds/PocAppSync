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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobile.client.AWSMobileClient
import com.amplifyframework.api.ApiException
import com.amplifyframework.api.ApiOperation
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.api.graphql.model.ModelSubscription
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.model.temporal.Temporal.Timestamp.*
import com.amplifyframework.datastore.generated.model.GameRoom
import com.amplifyframework.datastore.generated.model.Player
import java.time.temporal.Temporal
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : Activity() {
    var players : ArrayList<Player> = ArrayList<Player>();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)


        // crea stanza e ci associa un giocatore
        findViewById<Button>(R.id.button).setOnClickListener {
            Log.i("idk", AWSMobileClient.getInstance().tokens.idToken.tokenString)
            val options: RestOptions = RestOptions.builder()
                .addHeader("Authorization", AWSMobileClient.getInstance().tokens.idToken.tokenString)
                .addPath("/testapi")
                .build()

            val gameroom: GameRoom = GameRoom.builder().build()
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






        // aggiorna punteggio di "alberto" che sarebbe nel nostro caso "noi stessi"
        findViewById<Button>(R.id.changescore).setOnClickListener {
            var old = players.stream().filter{ p-> p.name.toLowerCase(Locale.ROOT).indexOf("alberto") != -1}.findFirst().get()
            var alberto = Player.builder()
                .name(old.name)
                .id(old.id)
                .gameroom(old.gameroom)
                .score((Math.random() * 10000).toInt())
                .lastinteraction(now())
                .build()
            Amplify.API.mutate(
                ModelMutation.update(alberto),
                { response -> Log.i("MyAmplifyApp", "Todo with id: " + response.data.id) },
                { error -> Log.e("MyAmplifyApp", "Create failed", error) }
            )
        }


        // ottieni i giocatori e una volta ottenuti, ascoltare le loro modifiche
        runOnUiThread{
            Amplify.API.query(
                ModelQuery.list(Player::class.java),
                { response ->
                    players = ArrayList(response.data.items.toList())
                    val subscription: ApiOperation<*>? = Amplify.API.subscribe(
                        ModelSubscription.onUpdate(Player::class.java),
                        { Log.i("ApiQuickStart", "Subscription established") },
                        { onUpdate ->
                            players.removeIf { p -> p.id == onUpdate.data.id }
                            players.add(onUpdate.data)
                            updateOutput()
                        },
                        { onFailure -> Log.e("ApiQuickStart", "Subscription failed", onFailure) },
                        { Log.i("ApiQuickStart", "Subscription completed") }
                    )
                },
                { error -> Log.e("MyAmplifyApp", "Query failure", error) }
            )

        }








        // ottieni i giocatori
        findViewById<Button>(R.id.showplayers).setOnClickListener{
            Amplify.API.query(
                ModelQuery.list(Player::class.java),
                { response ->
                    val output =
                        response.data.items.joinToString { p -> "nome : ${p.name}, score :${p.score}, ultima interazione: ${p.lastinteraction}\n" }
                    runOnUiThread{
                        findViewById<TextView>(R.id.output).text = output
                    }
                },
                { error -> Log.e("MyAmplifyApp", "Query failure", error) }
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
    // mostra i giocatori
    private fun updateOutput(){
        runOnUiThread{
            findViewById<TextView>(R.id.output).text = players.joinToString { p -> "nome : ${p.name}, score :${p.score}\n" }
        }
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