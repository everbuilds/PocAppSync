package com.webhostedui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.generated.model.Player
import com.webhostedui.model.GameSession
import java.util.*

class GameRoomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_room)
        val currentPlayer = GameSession.player;
        if(currentPlayer != null){
            findViewById<Button>(R.id.update_score).text = "Aggiorna punteggio di ${currentPlayer.name}"
            findViewById<Button>(R.id.update_score).setOnClickListener {
                Log.i("ID","${currentPlayer.name},${currentPlayer.id}")
                var player = Player.builder()
                    .name(currentPlayer.name)
                    .id(currentPlayer.id)
                    .gameroom(currentPlayer.gameroom)
                    .score((Math.random() * 10000).toInt())
                    .lastinteraction(Temporal.Timestamp.now())
                    .build()
                Amplify.API.mutate(
                    ModelMutation.update(player),
                    { response -> Log.i("MyAmplifyApp", "Todo with id: " + response.data.id) },
                    { error -> Log.e("MyAmplifyApp", "Create failed", error) }
                )
            }
            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    Amplify.API.query(
                        ModelQuery.list(
                            Player::class.java,
                            Player.GAMEROOM.eq(currentPlayer.gameroom.id)
                        ),
                        { response ->
                            runOnUiThread {
                                findViewById<TextView>(R.id.game_output).text =
                                    response.data.items.joinToString(separator = "\n") { p ->
                                        "nome : ${p.name} , score : ${p.score}"
                                    }
                            }
                            Log.i("idk", response.toString())
                        },
                        { error -> Log.e("MyAmplifyApp", "Query failure", error) }
                    )
                }
            }, 3000, 3000)
        }
    }
}