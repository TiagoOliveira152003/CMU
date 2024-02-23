package com.example.cmu_recurso

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cmu_recurso.ui.components.navigation.MyNavigatonDrawer
import com.example.cmu_recurso.ui.theme.CMU_RecursoTheme


class MainActivity : ComponentActivity() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var recreateRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CMU_RecursoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    val permissionsToCheck = arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )

                    var permissionsGranted by remember { mutableStateOf(false) }


                    LaunchedEffect(key1 = permissionsGranted) {
                        permissionsGranted = checkPermissions(context, *permissionsToCheck)
                        if (!permissionsGranted) {
                            requestPermissions(context as Activity, permissionsToCheck)
                        }
                    }


                    if(permissionsGranted){
                        if (!recreateRequested) {
                            recreateRequested=true
                            MyNavigatonDrawer()
                        }
                    }
                }
            }
        }
    }
    /**
     * Verifica se as permissões necessárias foram concedidas.
     * @param context O contexto da aplicação.
     * @param permissions As permissões a serem verificadas.
     * @return true se todas as permissões foram concedidas, false caso contrário.
     */
    private fun checkPermissions(context: Context, vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
    /**
     * Solicita as permissões necessárias ao utilizador.
     * @param activity A atividade onde as permissões serão solicitadas.
     * @param permissions As permissões a serem solicitadas.
     */
    private fun requestPermissions(activity: Activity, permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    /**
     * Se a solicitação foi para permissões de localização e qualquer uma das permissões foi negada,
     * a atividade é recriada para garantir que as permissões necessárias sejam concedidas corretamente.
     * @param requestCode O código da solicitação de permissão.
     * @param permissions As permissões solicitadas.
     * @param grantResults Os resultados das solicitações de permissão, indicando se cada permissão foi concedida ou não.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            val anyDenied = grantResults.any { it != PackageManager.PERMISSION_GRANTED }
            if (anyDenied) {
                recreate()
            }
        }
    }
}

