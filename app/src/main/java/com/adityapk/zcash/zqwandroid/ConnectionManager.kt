package com.adityapk.zcash.zqwandroid

import android.content.Intent
import android.util.Log
import com.beust.klaxon.json
import okhttp3.*
import okio.ByteString
import java.net.ConnectException
import java.util.concurrent.TimeUnit


object ConnectionManager {

    val DATA_SIGNAL: String = "ConnectionManager_NewData_Signal"

    /**
     * Refresh all data, including attempting a connection if none exists
     */
    fun refreshAllData() {
        // First, try to make a connection.

        makeConnection()
    }

    // Attempt a connection to the server. If there is no saved connection, we'll set the connection status
    // to None
    private fun makeConnection(directConn : Boolean = true) {
        val connString = DataModel.getConnString(ZQWApp.appContext!!)
        if (connString.isNullOrBlank()) {
            // The user might have just disconnected, so make sure we are disconnected

            DataModel.ws?.close(1000, "disconnected")
            sendUpdateDataSignal(true)
            return
        }

        println("MakeConnection")

        // If still connecting, this is a duplicate call, so do nothing but wait.
        if (DataModel.connStatus == DataModel.ConnectionStatus.CONNECTING) {
            return
        }

        // If already connected, then refresh data
        if (DataModel.connStatus == DataModel.ConnectionStatus.CONNECTED) {
            DataModel.makeAPICalls()
            return
        }

        println("Attempting new connection ${DataModel.connStatus}")

        // If direct connection, then connect to the URL in connection string
        if (directConn) {
            // Update status to connecting, so we can update the UI
            DataModel.connStatus = DataModel.ConnectionStatus.CONNECTING
            println("Connstatus = connecting")

            val client = OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS).build()
            val request = Request.Builder().url(connString).build()
            val listener = WebsocketClient(true)

            DataModel.ws = client.newWebSocket(request, listener)

            DataModel.makeAPICalls()
        } else {
            // Connect to the wormhole
            DataModel.connStatus = DataModel.ConnectionStatus.CONNECTING

            println("Connstatus = connecting")

            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
            val request = Request.Builder().url("ws://192.168.5.187:7070").build()
            val listener = WebsocketClient(false)

            DataModel.ws = client.newWebSocket(request, listener)

            DataModel.makeAPICalls()
        }
    }

    fun sendUpdateDataSignal(updateTxns: Boolean = false) {
        val i = Intent(DATA_SIGNAL)
        i.putExtra("action", "newdata")
        i.putExtra("updateTxns", updateTxns)
        ZQWApp.appContext?.sendBroadcast(i)
    }

    fun sendErrorSignal(msg: String? = null, doDisconnect: Boolean = false) {
        val i = Intent(DATA_SIGNAL)
        i.putExtra("action", "error")
        i.putExtra("msg", msg)
        i.putExtra("doDisconnect", doDisconnect)
        ZQWApp.appContext?.sendBroadcast(i)
    }


    private class WebsocketClient (directConn: Boolean) : WebSocketListener() {
        val m_directConn = directConn
        val TAG = "WebsocketClient"

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "Opened Websocket")
            DataModel.connStatus = DataModel.ConnectionStatus.CONNECTED
            println("Connstatus = connected")

            // If this is a connection to wormhole, we have to register ourselves
            if (!m_directConn) {
                if (!DataModel.getWormholeCode().isNullOrBlank())
                    webSocket.send( json { obj( "register" to DataModel.getWormholeCode()) }.toJsonString())
            }

            sendUpdateDataSignal()
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            Log.i(TAG, "Receiving $text")

            val r = DataModel.parseResponse(text!!)

            if (r.displayMsg != null) {
                sendErrorSignal(r.displayMsg, r.doDisconnect)

                if (r.doDisconnect) {
                    webSocket?.close(1000, "Peer Error caused disconnect")
                }

            } else {
                sendUpdateDataSignal(r.updateTxns)
            }
        }

        override fun onMessage(webSocket: WebSocket?, bytes: ByteString) {
            Log.i(TAG, "Receiving bytes : " + bytes.hex())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String?) {
            DataModel.connStatus = DataModel.ConnectionStatus.DISCONNECTED
            println("Connstatus = disconnected")

            Log.i(TAG,"Closing : $code / $reason")
            if (code == 1001) {
                sendErrorSignal(reason, true)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG,"Failed $t")
            DataModel.connStatus = DataModel.ConnectionStatus.DISCONNECTED

            // If the connection is direct, and there is no need to connect to
            val allowInternetConnect = true
            if (t is ConnectException && (m_directConn && !allowInternetConnect)) {
                sendErrorSignal(t.localizedMessage, true)
            }

            // If this was a direct connection and there was a failure to connect, retry connecting
            // without the direct connection (i.e., through wormhole)
            if (m_directConn && allowInternetConnect) {
                makeConnection(false)
            }
        }
    }
}