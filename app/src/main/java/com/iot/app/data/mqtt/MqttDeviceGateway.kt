package com.iot.app.data.mqtt

import android.content.Context
import android.util.Log
import com.iot.app.data.gateway.DeviceCommandGateway
import com.iot.app.model.DeviceCommand
import com.iot.app.model.DeviceState
import com.iot.app.model.DeviceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap

/**
 * MQTT Implementation of DeviceCommandGateway using Eclipse Paho.
 * 
 * ARCHITECTURE:
 * - Singleton pattern (one connection per app instance)
 * - Runs entirely off main thread (Dispatchers.IO)
 * - Automatic reconnection on connection loss
 * - Thread-safe state management
 * - Clean lifecycle handling
 * 
 * MQTT FEATURES:
 * - QoS 1 for commands (at least once delivery)
 * - QoS 1 for state/status subscriptions
 * - Last Will Testament on /status topic
 * - Clean session = false (persist subscriptions)
 * - Auto-reconnect with exponential backoff
 * 
 * THREADING:
 * - All public methods are suspend/Flow-based
 * - Internal MQTT callbacks run on Paho thread pool
 * - Flow emissions switch to Dispatchers.IO
 * - No UI thread blocking
 * 
 * TESTING WITHOUT HARDWARE:
 * - Use EMQX/HiveMQ web client to simulate devices
 * - Use MQTT.fx or MQTTBox desktop clients
 * - Use another Android MQTT app
 */
class MqttDeviceGateway private constructor(
    context: Context,
    private val brokerUrl: String = "tcp://broker.emqx.io:1883",
    private val clientId: String = "SmartHomeApp_${System.currentTimeMillis()}"
) : DeviceCommandGateway {
    
    companion object {
        private const val TAG = "MqttDeviceGateway"
        
        // Default public MQTT broker for testing (change to your broker)
        private const val DEFAULT_BROKER_URL = "tcp://broker.emqx.io:1883"
        
        // QoS levels
        private const val QOS_COMMAND = 1  // At least once
        private const val QOS_STATE = 1    // At least once
        private const val QOS_STATUS = 1   // At least once
        
        // Connection settings
        private const val CONNECTION_TIMEOUT = 30  // seconds
        private const val KEEP_ALIVE_INTERVAL = 60 // seconds
        private const val MAX_RECONNECT_DELAY = 60000L // 1 minute
        
        @Volatile
        private var instance: MqttDeviceGateway? = null
        
        /**
         * Get singleton instance
         */
        fun getInstance(context: Context, brokerUrl: String = DEFAULT_BROKER_URL): MqttDeviceGateway {
            return instance ?: synchronized(this) {
                instance ?: MqttDeviceGateway(
                    context.applicationContext,
                    brokerUrl
                ).also { instance = it }
            }
        }
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appContext = context.applicationContext
    
    // MQTT client
    private var mqttClient: MqttAsyncClient? = null
    
    // Shared flows for state and status updates
    // These are hot flows that multiple observers can collect from
    private val stateFlow = MutableSharedFlow<Pair<String, DeviceState>>(replay = 0, extraBufferCapacity = 100)
    private val statusFlow = MutableSharedFlow<DeviceStatus>(replay = 0, extraBufferCapacity = 100)
    
    // Track subscriptions to avoid duplicate subscribes
    private val subscribedTopics = ConcurrentHashMap.newKeySet<String>()
    
    // Connection state
    @Volatile
    private var connected = false
    
    /**
     * Connect to MQTT broker
     */
    override suspend fun connect() = withContext(Dispatchers.IO) {
        if (connected && mqttClient?.isConnected == true) {
            Log.d(TAG, "Already connected")
            return@withContext
        }
        
        try {
            Log.d(TAG, "Connecting to MQTT broker: $brokerUrl")
            
            // Create MQTT client
            mqttClient = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
            
            // Set callback for incoming messages
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "Connection lost: ${cause?.message}")
                    connected = false
                    scope.launch {
                        attemptReconnect()
                    }
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    handleIncomingMessage(topic, message)
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Command delivered successfully
                }
            })
            
            // Configure connection options
            val options = MqttConnectOptions().apply {
                isCleanSession = false  // Persist subscriptions
                connectionTimeout = CONNECTION_TIMEOUT
                keepAliveInterval = KEEP_ALIVE_INTERVAL
                isAutomaticReconnect = true
                
                // Last Will Testament: publish offline status when connection is lost unexpectedly
                val lwt = MqttStatusPayload(online = false)
                val lwtTopic = "smarthome/app/status"  // App-level status
                setWill(lwtTopic, MqttJson.encodeStatus(lwt).toByteArray(), QOS_STATUS, true)
            }
            
            // Connect asynchronously
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i(TAG, "Connected to MQTT broker")
                    connected = true
                    scope.launch {
                        subscribeToAllTopics()
                    }
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failed: ${exception?.message}", exception)
                    connected = false
                    scope.launch {
                        attemptReconnect()
                    }
                }
            })
            
        } catch (e: MqttException) {
            Log.e(TAG, "MQTT connection error: ${e.message}", e)
            connected = false
            throw e
        }
    }
    
    /**
     * Disconnect from MQTT broker
     */
    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Disconnecting from MQTT broker")
            mqttClient?.disconnect()
            mqttClient?.close()
            mqttClient = null
            connected = false
            subscribedTopics.clear()
        } catch (e: MqttException) {
            Log.e(TAG, "Disconnect error: ${e.message}", e)
        }
    }
    
    /**
     * Check connection status
     */
    override fun isConnected(): Boolean = connected && mqttClient?.isConnected == true
    
    /**
     * Send command to device
     */
    override suspend fun sendCommand(
        homeId: String,
        roomId: String,
        deviceId: String,
        command: DeviceCommand
    ) = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            Log.w(TAG, "Cannot send command - not connected")
            return@withContext
        }
        
        try {
            val topic = MqttTopicBuilder.buildCommandTopic(homeId, roomId, deviceId)
            val payload = when (command) {
                is DeviceCommand.TurnOn -> MqttCommandPayload(
                    action = MqttCommandPayload.ACTION_ON
                )
                is DeviceCommand.TurnOff -> MqttCommandPayload(
                    action = MqttCommandPayload.ACTION_OFF
                )
                is DeviceCommand.SetValue -> MqttCommandPayload(
                    action = MqttCommandPayload.ACTION_SET_VALUE,
                    value = command.value
                )
                is DeviceCommand.RequestState -> MqttCommandPayload(
                    action = MqttCommandPayload.ACTION_REQUEST_STATE
                )
            }
            
            val jsonPayload = MqttJson.encodeCommand(payload)
            val mqttMessage = MqttMessage(jsonPayload.toByteArray()).apply {
                qos = QOS_COMMAND
                isRetained = false
            }
            
            Log.d(TAG, "Publishing command to $topic: $jsonPayload")
            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Command published successfully")
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Command publish failed: ${exception?.message}")
                }
            })
            
        } catch (e: MqttException) {
            Log.e(TAG, "Error sending command: ${e.message}", e)
        }
    }
    
    /**
     * Observe device state changes
     */
    override fun observeDeviceState(deviceId: String): Flow<DeviceState> {
        return stateFlow
            .filter { (id, _) -> id == deviceId }
            .map { (_, state) -> state }
    }
    
    /**
     * Observe device status changes
     */
    override fun observeDeviceStatus(deviceId: String): Flow<DeviceStatus> {
        return statusFlow.filter { it.deviceId == deviceId }
    }
    
    /**
     * Subscribe to all state and status topics with wildcards
     */
    private suspend fun subscribeToAllTopics() {
        withContext(Dispatchers.IO) {
            try {
                val stateWildcard = MqttTopicBuilder.buildAllStatesWildcard()
                val statusWildcard = MqttTopicBuilder.buildAllStatusWildcard()
            
                // Subscribe to all state topics
                if (subscribedTopics.add(stateWildcard)) {
                    mqttClient?.subscribe(stateWildcard, QOS_STATE, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i(TAG, "Subscribed to: $stateWildcard")
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Subscribe failed: ${exception?.message}")
                            subscribedTopics.remove(stateWildcard)
                        }
                    })
                }
                
                // Subscribe to all status topics
                if (subscribedTopics.add(statusWildcard)) {
                    mqttClient?.subscribe(statusWildcard, QOS_STATUS, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i(TAG, "Subscribed to: $statusWildcard")
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Subscribe failed: ${exception?.message}")
                            subscribedTopics.remove(statusWildcard)
                        }
                    })
                }
                Unit  // Explicit Unit return to avoid if-expression issues
            } catch (e: MqttException) {
                Log.e(TAG, "Subscription error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Handle incoming MQTT messages
     */
    private fun handleIncomingMessage(topic: String?, message: MqttMessage?) {
        if (topic == null || message == null) return
        
        scope.launch {
            try {
                val payload = String(message.payload)
                Log.d(TAG, "Message arrived on $topic: $payload")
                
                when {
                    topic.endsWith("/state") -> handleStateMessage(topic, payload)
                    topic.endsWith("/status") -> handleStatusMessage(topic, payload)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling message: ${e.message}", e)
            }
        }
    }
    
    /**
     * Handle state update message
     */
    private suspend fun handleStateMessage(topic: String, payload: String) {
        val deviceId = MqttTopicBuilder.extractDeviceId(topic) ?: return
        val statePayload = MqttJson.decodeState(payload) ?: return
        
        val deviceState = when {
            statePayload.isOn == true -> DeviceState.On
            statePayload.isOn == false -> DeviceState.Off
            statePayload.value != null -> DeviceState.Value(statePayload.value)
            else -> return
        }
        
        stateFlow.emit(deviceId to deviceState)
    }
    
    /**
     * Handle status update message
     */
    private suspend fun handleStatusMessage(topic: String, payload: String) {
        val deviceId = MqttTopicBuilder.extractDeviceId(topic) ?: return
        val statusPayload = MqttJson.decodeStatus(payload) ?: return
        
        val deviceStatus = DeviceStatus(
            deviceId = deviceId,
            online = statusPayload.online
        )
        
        statusFlow.emit(deviceStatus)
    }
    
    /**
     * Attempt to reconnect with exponential backoff
     */
    private suspend fun attemptReconnect() {
        var delay = 1000L
        
        while (!connected && delay <= MAX_RECONNECT_DELAY) {
            Log.d(TAG, "Attempting reconnect in ${delay}ms")
            kotlinx.coroutines.delay(delay)
            
            try {
                connect()
                if (connected) {
                    Log.i(TAG, "Reconnection successful")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect failed: ${e.message}")
            }
            
            delay = (delay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
        }
    }
}
