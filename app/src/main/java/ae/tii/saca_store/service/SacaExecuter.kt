package ae.tii.saca_store.service

import ae.tii.saca_store.BuildConfig
import ae.tii.saca_store.util.repeatUntilNotNull
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.android.google.gce.gceservice.ISacaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class SacaExecuter(private val context: Context) : ServiceConnection {
    private var sacaService: ISacaService? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null


    fun bindToService(): Boolean {
        Log.i(TAG, "Trying to bind to SACA_SERVICE")
        val intent = Intent()
        intent.setClassName(BuildConfig.SACA_SERVICE_PKG_NAME, BuildConfig.SACA_SERVICE_CLASS_NAME)
        return context.bindService(intent, this, BIND_AUTO_CREATE)
    }

    fun unbindToService() {
        context.unbindService(this)
    }

    private fun getCvdAccessToken() {
        job?.cancel()
        job = scope.launch {
            if (sacaService != null) {
                val cvdToken = repeatUntilNotNull(times = 5) { sacaService?.accessToken }
                Log.i(TAG, "CVD-Token: $cvdToken")
            } else {
                bindToService()
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
        getCvdAccessToken()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.i(TAG, "Disconnected from SACA credentials service")
        sacaService = null
        job?.cancel()
    }

    override fun onBindingDied(name: ComponentName?) {
        super.onBindingDied(name)
        Log.i(TAG, "onBindingDied")
        sacaService = null
        job?.cancel()
    }

    override fun onNullBinding(name: ComponentName?) {
        super.onNullBinding(name)
        Log.i(TAG, "onNullBinding")
        sacaService = null
        job?.cancel()
    }
    //endregion


    companion object {
        private const val TAG = "SacaExecuter"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SacaExecuter? = null

        fun getInstance(context: Context): SacaExecuter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SacaExecuter(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}