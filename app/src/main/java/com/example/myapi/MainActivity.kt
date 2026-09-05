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
import com.example.myapi.Data.Result.Results
import com.example.myapi.ui.theme.MyAPITheme

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //判斷回傳結果是否為空
            //如果 intent.extras?.getString("json") 的結果是 null，就直接終止目前這個函式（Function）的執行，不再往下走。
            val json = intent.extras?.getString("json")?: return
            //解析Intent取得JSON字串，把json物件以Data格式做轉換
            // 宣告你要解析成 List<Results>
            /*
            TypeToken<List<Results>>()：這是 Gson 提供的一個抽象類別。透過泛型 <List<Results>>，我們精確地告訴程式：「我要找的是一個 List，裡面的元素型別是 Results」。
            object : ... {}：這是 Kotlin 的 物件表達式（Object Expression），用來建立一個匿名內部類別（Anonymous Inner Class）的實例。為什麼要用 object :？因為 TypeToken 的建構子受保護，且 Gson 需要透過這個匿名類別去「抓取」並保留泛型的實際型別（繞過編譯期的型別擦除）。
            .type：這是 TypeToken 類別的一個屬性，會回傳一個 java.lang.reflect.Type 物件。這個物件記錄了剛剛指定的 List<Results> 詳細型別資訊，正是 Gson 解析時所需要的參數。
            */
            val listType = object : TypeToken<List<Results>>() {}.type
            val data: List<Results> = Gson().fromJson(json, listType)

            //建立一個型別為 String?（可為空）、大小等於 data.size 的陣列，而且這個陣列剛被建立時，裡面的每一個格子全部都是 null
            val items = arrayOfNulls<String>(data.size)
            //建立一個字串陣列，用於提取『站名』與『目的地』資訊
            for(i in 0 until data.size)
                items[i] = "\n列車即將進入 :${data[i].Station}" +
                        "\n列車行駛目的地 :${data[i].Destination}"
            //使用者介面的操作必須在UI Thread上執行
            this@MainActivity.runOnUiThread {
                //使用Dialog呈現結果
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("台北捷運列車到站站名")
                    .setItems(items) { dialogInterface, i ->
                        dialogInterface.dismiss()
                    }
                    .show()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        //註冊Receiver，用來接收Http Response
        // 如果這個廣播只允許「你自己的 App 內部」發送與接收（最常見、最安全）：
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter("MyMessage"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val btn_query = findViewById<Button>(R.id.btn_query)
        btn_query.setOnClickListener {
            //建立一個Request物件，並使用url()方法加入URL
            val req = Request.Builder().url("https://tcgmetro.blob.core.windows.net/stationnames/stations.json").build()
            //建立okHttpClient物件，newCall()送出請求，enqueue()接收回傳
            OkHttpClient().newCall(req).enqueue(object: Callback {
                //發送成功執行此方法
                override fun onResponse(call: Call, response: Response) {
                    //判斷伺服器回傳狀態
                    when{
                        response.code ==200 ->{
                            //判斷回傳是否為空
                            val json = response.body?.string()?:return
                            //取得用response的回傳結果（Json字串），並使用廣播發送
                            val intent = Intent("MyMessage").apply {
                                putExtra("json", json)
                                setPackage(packageName) // 綁定自己的包名，把它變成「明確意圖」
                            }
                            sendBroadcast(intent)
                        }
                        !response.isSuccessful ->Log.e("伺服器錯誤","${response.code} ${response.message}")
                        else ->Log.e("其他錯誤","${response.code} ${response.message}")
                    }
                }
                //發送失敗執行此方法
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("查詢失敗","$e")
                }
            })
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        //註銷Receiver
        unregisterReceiver(receiver)
    }
}

class Data {
    lateinit var result: Result

    class Result {
        lateinit var results : Array<Results>

        class Results {
            val Station = ""    //站名
            val Destination = ""    //目的地
        }
    }
}