package com.webhostedui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.amplifyframework.api.ApiOperation
import com.amplifyframework.api.graphql.model.ModelSubscription
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.GameRoom
import com.amplifyframework.datastore.generated.model.Player
import com.webhostedui.model.GameSession

class WaitingRoom : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_room)
        val idPlayer = intent.getStringExtra("id-player")
        if(idPlayer != null) {
            var sub: ApiOperation<*>? = null;
            sub = Amplify.API.subscribe(
                ModelSubscription.onCreate(Player::class.java),
                { Log.i("ApiQuickStart", "Subscription established") },
                { onCreated ->
                    Log.i("idk", onCreated.data.toString())
                    if(onCreated.data.id.equals(idPlayer)){
                        Log.i("idk","È la mia partitaaaaa")
                        GameSession.player = onCreated.data
                        runOnUiThread{
                            sub!!.cancel()
                        }
                        startActivity(Intent(this, GameRoomActivity::class.java))
                    }
                },
                { onFailure -> Log.e("ApiQuickStart", "Subscription failed", onFailure) },
                { Log.i("ApiQuickStart", "Subscription completed") }
            )
        }
    }
}