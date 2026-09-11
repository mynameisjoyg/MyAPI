package com.example.myapi

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.myapi.ui.theme.MyAPITheme

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val receiver: BroadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            var json = intent?.extras?.getString("json")?:return
            var myType = object: TypeToken<List<String>>(){}.type
            var data: List<String> = Gson().fromJson(json, myType)

            var items = arrayOfNulls<String>(data.size)
            for(i in 0 until data.size){
                items[i]="Subject: ${data[i]}"
            }
            AlertDialog.Builder(this@MainActivity).setItems(items) {
                dialogInterface, i -> dialogInterface.dismiss()
            }.show()

        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        var bt_query = findViewById<Button>(R.id.btn_query)
        bt_query.setOnClickListener {
            var req = Request.Builder().url("https://demo2-22z2.onrender.com/myStudy").build()
            OkHttpClient().newCall(req).enqueue(object: Callback{
                override fun onFailure(call: Call, e: okio.IOException) {
                    TODO("Not yet implemented")
                }

                override fun onResponse(call: Call, response: Response) {
                    when {
                        response.code == 200 -> {
                            var json = response.body.string()?:return
                            var intent = Intent("MyMessage").apply {
                                putExtra("json", json)
                                setPackage(packageName)
                            }
                            sendBroadcast(intent)
                        }
                    }
                }

            })
        }
        ContextCompat.registerReceiver(this, receiver, IntentFilter("MyMessage"), ContextCompat.RECEIVER_NOT_EXPORTED)

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

data class SubjectResponse(
    val allSubjects: List<String>
)