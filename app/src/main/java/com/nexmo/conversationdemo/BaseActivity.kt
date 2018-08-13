package com.nexmo.conversationdemo

import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import org.jetbrains.anko.runOnUiThread

open class BaseActivity : AppCompatActivity() {
    protected val TAG = "Nexmo Stitch Demo: "

    protected fun logAndShow(message: String?) {
        Log.d(TAG, message)
        runOnUiThread{ Toast.makeText(this@BaseActivity, message, Toast.LENGTH_SHORT).show() }
    }
}
