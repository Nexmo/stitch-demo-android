package com.nexmo.conversationdemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.nexmo.sdk.conversation.client.Call
import com.nexmo.sdk.conversation.client.ConversationClient
import com.nexmo.sdk.conversation.client.event.NexmoAPIError
import com.nexmo.sdk.conversation.client.event.RequestHandler
import kotlinx.android.synthetic.main.activity_call.*

class CallActivity : AppCompatActivity(), RequestHandler<Call>  {
    private val TAG = CallActivity::class.simpleName
    private lateinit var conversationClient: ConversationClient
    private var currentCall: Call? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        conversationClient = (application as ConversationClientApplication).conversationClient

        callControlBtn.setOnClickListener {
            callPhone()
        }
    }

    fun callPhone() {
        var phoneNumber = phoneNumberInput.text.toString()

        conversationClient.callPhone(phoneNumber, object : RequestHandler<Call> {
            override fun onError(apiError: NexmoAPIError) {
                logAndShow("Cannot initiate call: " + apiError.message)
                Log.e(TAG, "onError: ", apiError)
            }

            override fun onSuccess(result: Call) {
                currentCall = result
                callControlBtn.setOnClickListener { endCall() }
                when (result.callState) {
                    Call.CALL_STATE.STARTED -> logAndShow("Started")
                    Call.CALL_STATE.RINGING -> logAndShow("Ringing")
                    Call.CALL_STATE.ANSWERED -> logAndShow("Answered")
                    else -> logAndShow("Error attaching call listener")
                }

            }
        })
    }

    private fun endCall() {
        currentCall?.hangup(object : RequestHandler<Void> {
            override fun onError(apiError: NexmoAPIError) {
                logAndShow("Cannot hangup: " + apiError.toString())
            }

            override fun onSuccess(result: Void) {
                logAndShow("Call completed.")
                callControlBtn.setOnClickListener { callPhone() }
            }
        })
    }

    override fun onSuccess(result: Call?) {
        callStatus.text = "In call with ${phoneNumberInput.text}"
        currentCall = result
        callControlBtn.setOnClickListener { endCall() }
    }


    override fun onError(apiError: NexmoAPIError?) {
        logAndShow(apiError?.message)
    }

    private fun logAndShow(message: String?) {
        Log.d(TAG, message)
        runOnUiThread { Toast.makeText(this@CallActivity, message, Toast.LENGTH_SHORT).show() }
    }
}
