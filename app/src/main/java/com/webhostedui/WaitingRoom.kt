package com.webhostedui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.amplifyframework.api.ApiOperation
import com.amplifyframework.api.graphql.model.ModelSubscription
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Player

class WaitingRoom : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_room)
        var sub: ApiOperation<*>? = null;
        sub= Amplify.API.subscribe(
            ModelSubscription.onCreate(Player::class.java),
            { Log.i("ApiQuickStart", "\n\n\n\n\n\n\n\n\n\n\n\n\nSubscription established") },
            { onCreated ->
                Log.i("TESTTTT", onCreated.toString())
                if(onCreated.data.name.equals(Amplify.Auth.currentUser.username, ignoreCase = true)){
                    onCreated.data.gameroom.id
                    Log.i("io", onCreated.data.toString())
                    sub!!.cancel()
                }
                Log.i("ApiQuickStart", "\n\n\n\n\n\n\n\n\n\n\n\n\nTodo create subscription received: " + onCreated.data) },
            { onFailure -> Log.e("ApiQuickStart", "\n\n\n\n\n\n\n\n\n\n\n\n\nSubscription failed", onFailure) },
            { Log.i("ApiQuickStart", "\n\n\n\n\n\n\n\n\n\n\n\n\nSubscription completed") }
        )
    }
}