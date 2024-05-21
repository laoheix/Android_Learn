package com.laohei.permission_dome

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.laohei.permission_dome.ui.theme.AndroidLearnTheme

class MainActivity : ComponentActivity() {
    companion object {
        private var TAG = MainActivity::class.simpleName
        private const val DBG = true
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidLearnTheme {
                val context = LocalContext.current
                var readStorageText by remember {
                    mutableStateOf("")
                }
                val readStoragePermission = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val imageUrl =
                    remember { "https://i0.hdslb.com/bfs/archive/2b6b79dfd8891a70bca2f66b44bed000c9443448.jpg@672w_378h_1c_!web-home-common-cover.avif" }
                val internetPermission = remember { Manifest.permission.INTERNET }
                val permissionState = rememberPermissionState(permission = internetPermission)

                val cameraPermissions = remember {
                    buildList {
                        add(Manifest.permission.CAMERA)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
                        }
                    }.toList()
                }

                val permissionsState =
                    rememberMultiplePermissionsState(permissions = cameraPermissions)
                var mCameraUri: Uri? = null
                var imageUri by remember {
                    mutableStateOf<Uri?>(null)
                }
                val cameraLauncher =
                    rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
                        if (it) {
                            imageUri = mCameraUri
                            if (DBG) {
                                Log.d(TAG, "onCreate: ${imageUri?.path}")
                            }
                        }
                    }


                PermissionChecked(
                    permission = readStoragePermission,
                    failed = {
                        readStorageText = "$readStoragePermission authorization failed"
                        if (DBG) {
                            Log.d(TAG, "onCreate: $readStoragePermission")
                        }
                    }
                ) {
                    readStorageText = "$readStoragePermission authorization succeed"
                    if (DBG) {
                        Log.d(TAG, "onCreate: $readStoragePermission")
                    }
                }


                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Greeting(
                            name = "Android",
                        )
                        Text(text = readStorageText)

                        if (permissionState.status.isGranted) {
                            AsyncImage(model = imageUrl, contentDescription = "image")
                        } else {
                            Button(onClick = { permissionState.launchPermissionRequest() }) {
                                Text(text = "授权网络")
                            }
                        }

                        Button(onClick = {
                            if (permissionsState.allPermissionsGranted) {
                                mCameraUri = context.contentResolver.insert(
                                    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI else
                                        MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                                    ContentValues()
                                )
                                if (mCameraUri != null) {
                                    cameraLauncher.launch(mCameraUri!!)
                                }
                            } else {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        }) {
                            Text(text = if (permissionsState.allPermissionsGranted) "打开相机" else "授权相机")
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUri)
                                .error(R.drawable.ic_launcher_foreground)
                                .crossfade(300)
                                .build(),
                            contentDescription = "camera"
                        )
                    }

                }
            }
        }
    }
}


@Composable
fun PermissionChecked(permission: String, failed: () -> Unit, succeed: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            if (it) {
                succeed()
            } else {
                failed()
            }
        }
    val lifecycleObserver = remember {
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (permission.isGranted(context)) {
                    succeed()
                } else {
                    launcher.launch(permission)
                }
            }
        }
    }

    DisposableEffect(lifecycle, lifecycleObserver) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

}

private fun String.isGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.checkSelfPermission(this) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidLearnTheme {
        Greeting("Android")
    }
}