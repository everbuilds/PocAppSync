package com.webhostedui

import android.R.attr.*
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

//import okhttp3.OkHttp

class MainActivity : Activity() {
    var players : ArrayList<Player> = ArrayList<Player>();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)

        // crea stanza e ci associa un giocatore (richiesta HTTP con amplify)
        findViewById<Button>(R.id.requestAPI).setOnClickListener {
            Log.i("idk", AWSMobileClient.getInstance().tokens.idToken.tokenString)
            /*val options: RestOptions = RestOptions.builder()
                .addHeader("Authorization", AWSMobileClient.getInstance().tokens.idToken.tokenString)
                .addPath("https://rjgw85764l.execute-api.us-east-2.amazonaws.com/V1/")
                .addBody(Amplify.Auth.currentUser.username.toByteArray())
                .build()*/

            val url = "https://rjgw85764l.execute-api.us-east-2.amazonaws.com/V1/"
            val queue  = Volley.newRequestQueue(this)
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET, url, JSONObject(Amplify.Auth.currentUser.username),
                Response.Listener { response ->
                    val id = "Response: %s".format(response.toString())
                    Log.i("IDrequestAPI", "${id}")
                },
                Response.ErrorListener { error ->
                    Log.i("IDrequestAPI", "Errore cnnessione Json")
                }
            )


            queue.add(jsonObjectRequest)


            val gameroom: GameRoom = GameRoom.builder().build()
            Amplify.API.mutate(
                ModelMutation.create(gameroom),
                { response ->
                    Log.i("MyAmplifyApp", "Added GameRoom with id: " + response.getData().getId())
                    val player = Player.builder()
                        .name(Amplify.Auth.currentUser.username)
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






        // aggiorna il proprio punteggio
        findViewById<Button>(R.id.changescore).setOnClickListener {
            var old = players.stream().filter{ p-> p.name.toLowerCase(Locale.ROOT).indexOf(Amplify.Auth.currentUser.username.toLowerCase()) != -1}.findFirst().get()
            Log.i("ID","${old.name},${old.id}")
            var player = Player.builder()
                .name(old.name)
                .id(old.id)
                .gameroom(old.gameroom)
                .score((Math.random() * 10000).toInt())
                .lastinteraction(now())
                .build()
            Amplify.API.mutate(
                ModelMutation.update(player),
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
                        ModelSubscription.onUpdate(Player::class.java),                         //_______________________
                        { Log.i("ApiQuickStart", "Subscription established") },
                        { onUpdate ->                                                           //----update.data() getPlayers()
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
                        response.data.items.sortedBy { p -> p.name.toLowerCase() }.joinToString { p -> "\n\n ${p.name},  score :${p.score},  ultima interazione: ${p.lastinteraction}" }
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
            findViewById<TextView>(R.id.output).text = players.sortedBy { p -> p.score }.joinToString { p -> "nome : ${p.name}, score :${p.score}\n" }
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
                    findViewById<Button>(R.id.play).visibility = View.VISIBLE
                    findViewById<Button>(R.id.requestAPI).visibility = View.VISIBLE
                    findViewById<Button>(R.id.showplayers).visibility = View.VISIBLE
                    findViewById<Button>(R.id.changescore).visibility = View.VISIBLE
                    findViewById<Button>(R.id.changescore).text = "Cambia score di ${Amplify.Auth.currentUser.username}"
                } else {
                    findViewById<Button>(R.id.logout).visibility = View.GONE
                    findViewById<Button>(R.id.login).visibility = View.VISIBLE
                    findViewById<Button>(R.id.registrazione).visibility = View.VISIBLE
                    findViewById<Button>(R.id.play).visibility = View.GONE
                    findViewById<Button>(R.id.requestAPI).visibility = View.GONE
                    findViewById<Button>(R.id.showplayers).visibility = View.GONE
                    findViewById<Button>(R.id.changescore).visibility = View.GONE
                }
            } catch (ex: Exception) {
                findViewById<Button>(R.id.logout).visibility = View.GONE
                findViewById<Button>(R.id.login).visibility = View.VISIBLE
                findViewById<Button>(R.id.registrazione).visibility = View.VISIBLE

            }
        }
    }


}