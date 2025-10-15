package ae.tii.saca_store.service

import ae.tii.saca_store.BuildConfig
import ae.tii.saca_store.util.repeatUntilNotNull
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.android.google.gce.gceservice.ISacaService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SacaExecuter @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ServiceConnection {
    private var pendingContinuation: CancellableContinuation<String>? = null
    private var sacaService: ISacaService? = null

    private fun bindToService(): Boolean {
        Log.i(TAG, "Trying to bind to SACA_SERVICE")
        val intent = Intent()
        intent.setClassName(BuildConfig.SACA_SERVICE_PKG_NAME, BuildConfig.SACA_SERVICE_CLASS_NAME)
        return context.bindService(intent, this, BIND_AUTO_CREATE)
    }

    fun unbindToService() {
        context.unbindService(this)
        sacaService = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getCvdAccessToken(): String = suspendCancellableCoroutine { continuation ->
        if (sacaService != null) {
            val cvdToken = repeatUntilNotNull(times = 5) { sacaService?.accessToken } ?: ""
            Log.d(TAG, "getCvdAccessToken: cvdToken received")
            continuation.resume(cvdToken, onCancellation = null)
        } else {
            pendingContinuation = continuation
            val bind = bindToService()
            if (bind.not()) {
                Log.d(TAG, "Failed to bind SACA service")
                continuation.resume("", onCancellation = null)
            }
        }
    }

    //region ServiceConnection Listener callbacks
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        sacaService = ISacaService.Stub.asInterface(service)

        if (sacaService == null) {
            Log.i(TAG, "Invalid SACA credentials service binding")
            return
        }
        Log.i(TAG, "Connected to SACA credentials service: $sacaService")
        val cvdToken = repeatUntilNotNull(times = 5) { sacaService?.accessToken } ?: ""
        Log.d(TAG, "onServiceConnected: cvdToken received")
        pendingContinuation?.resume(cvdToken, onCancellation = null)
        pendingContinuation = null
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.i(TAG, "Disconnected from SACA credentials service")
        sacaService = null
        pendingContinuation?.cancel()
        pendingContinuation = null
    }

    override fun onBindingDied(name: ComponentName?) {
        super.onBindingDied(name)
        Log.i(TAG, "onBindingDied")
        sacaService = null
        pendingContinuation?.cancel()
        pendingContinuation = null
    }

    override fun onNullBinding(name: ComponentName?) {
        super.onNullBinding(name)
        Log.i(TAG, "onNullBinding")
        sacaService = null
        pendingContinuation?.cancel()
        pendingContinuation = null
    }
    //endregion


    companion object {
        private const val TAG = "SacaExecuter"
    }
}