package com.example.cmu_recurso.ui.components.screens


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.cmu_recurso.R
import com.example.cmu_recurso.data.ApiClient
import com.example.cmu_recurso.data.ApiService
import com.example.cmu_recurso.data.Category
import com.example.cmu_recurso.data.Post
import com.example.cmu_recurso.viewmodel.PostViewModel
import com.example.cmu_recurso.viewmodel.ProfileViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.IOException
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.storage
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Tela de criação de posts.
 *
 * @param navController Controlador de navegação para navegar entre as telas.
 */
@Composable
fun CreatePost(navController: NavHostController) {
    CreatePostScreen(navController)
}
/**
 * Tela de criação de um post.
 *
 * @param navController Controlador de navegação do Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(navController: NavHostController) {
    val profileViewModel: ProfileViewModel = viewModel()
    val ctx = LocalContext.current
    val postsViewModel: PostViewModel = viewModel()
    var showSnackbar by remember { mutableStateOf(false) }
    val titleState = rememberSaveable { mutableStateOf("") }
    val descriptionState = rememberSaveable { mutableStateOf("") }
    val photoUrlState = rememberSaveable { mutableStateOf("") }
    var addressState = ""
    var coordinatesState: String = ""
    val lifespanState = rememberSaveable { mutableStateOf(1) }
    val categoryState = rememberSaveable { mutableStateOf(Category.CURIOSIDADES) }

    val user = Firebase.auth.currentUser
    val isUserLoggedIn = user?.email != null
    var latMap by remember { mutableStateOf(0.0) }
    var lonMap by remember { mutableStateOf(0.0) }

    val locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)

    var selectedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageBitmap =
                    ctx.contentResolver.loadThumbnail(uri, Size(128, 128), null).asImageBitmap()
                uploadImageToFirebaseStorage(uri) { downloadUrl ->
                    photoUrlState.value = downloadUrl
                }
            }
        }

    val connectivityManager =
        LocalContext.current.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isConnected = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    if (isConnected) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isUserLoggedIn) {
                TextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    label = { Text("Title") }
                )

                TextField(
                    value = descriptionState.value,
                    onValueChange = { descriptionState.value = it },
                    label = { Text("Description") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text("Select Image")
                    }

                    selectedImageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(top = 16.dp, start = 8.dp)
                        )
                    }
                }
                if(categoryState.value!=Category.CURIOSIDADES) {
                    Slider(
                        value = lifespanState.value.toFloat(),
                        onValueChange = { lifespanState.value = it.toInt() },
                        valueRange = 1f..31f,
                        steps = 30,
                    ) {
                        Text(
                            text = lifespanState.value.toString(),
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Radio1(
                        selectedCategory = categoryState.value,
                        onCategorySelected = { categoryState.value = it }
                    )

                    Button(
                        onClick = {
                            var newCoordinatesState: String
                            var newAddressState: String
                            getCurrentLocationAndPrintInfo(
                                locationClient,
                                ctx
                            ) { locationInfo, latitude, longitude ->
                                latMap = latitude
                                lonMap = longitude
                                newCoordinatesState = locationInfo
                                getLocationInfo(latitude, longitude) { address ->
                                    newAddressState = address
                                    val emailTemp = user?.email ?: ""
                                    if(categoryState.value==Category.CURIOSIDADES){
                                        lifespanState.value=0
                                    }
                                    val newPost = Post(
                                        title = titleState.value,
                                        description = descriptionState.value,
                                        photoUrl = photoUrlState.value,
                                        address = newAddressState,
                                        coordinates = newCoordinatesState,
                                        lifespan = lifespanState.value.toLong(),
                                        category = categoryState.value,
                                        userEmail = emailTemp,
                                        comments = emptyList(),
                                        upvotedBy = mutableListOf(),
                                        downvotedBy = mutableListOf(),
                                        userRatings = mutableListOf()
                                    )


                                    postsViewModel.insertPost(newPost)

                                }
                            }
                            showSnackbar = true
                        },
                        modifier = Modifier.align(Alignment.CenterVertically)

                    ) {
                        Text(
                            text = "+",
                            fontSize = 24.sp
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    MapContent(locationClient, ctx)

                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Faça login para inserir um post",
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    TextButton(
                        onClick = { navController.navigate("Profile") },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "Fazer login",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                }
            }
        }
        if (showSnackbar) {
            LaunchedEffect(Unit) {
                delay(2000)
                showSnackbar = false
                navController.navigate("HomeScreen")
            }
            Snackbar(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(text = "Post adicionado com sucesso!")
            }
        }
    }else{
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "É necessário estar conectado à internet para criar um post",
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}
/**
 * Composable que exibe um grupo de botões para selecionar uma categoria.
 *
 * @param selectedCategory A categoria atualmente selecionada.
 * @param onCategorySelected Callback para ser chamado quando uma categoria for selecionada.
 */
@Composable
fun Radio1(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit
) {
    Column {
        RadioGroup(selectedCategory, onCategorySelected)
    }
}

/**
 * Composable que representa um grupo de botões.
 *
 * @param selectedCategory A categoria atualmente selecionada.
 * @param onCategorySelected Callback para ser chamado quando uma categoria for selecionada.
 */
@Composable
fun RadioGroup(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit
) {
    val categories = Category.values()

    categories.forEach { category ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            RadioButton(
                selected = (category == selectedCategory),
                onClick = { onCategorySelected(category) }
            )
            Text(text = category.name)
            Spacer(modifier = Modifier.width(4.dp)) // Espaço entre cada opção
            Spacer(modifier = Modifier.height(4.dp)) // Espaço entre cada opção

        }
    }
}

/**
 * Função para obter informações de localização com base nas coordenadas de latitude e longitude.
 *
 * @param latitude Latitude da localização.
 * @param longitude Longitude da localização.
 * @param callback Callback que será chamado após a obtenção das informações de localização, fornecendo o endereço correspondente.
 */
fun getLocationInfo(
    latitude: Double, longitude: Double, callback: (String) -> Unit
) {
    var address = ""

    val nominatim = ApiClient.getInstance().create(ApiService::class.java)
    nominatim.getLocationInfo(latitude, longitude)
        .enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                var body = response.body()
                if (body != null) {
                    address = body.getAsJsonPrimitive("display_name")?.asString.toString()
                    callback.invoke(
                        address
                    )
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                callback.invoke(
                    ""
                )
            }
        })
}

/**
 * Função para obter a localização atual do dispositivo e imprimir as informações relacionadas.
 *
 * @param locationClient Cliente para obter a localização do dispositivo.
 * @param context O contexto atual da aplicação.
 * @param callback Callback que será chamado após a obtenção da localização, fornecendo as informações de localização.
 */
fun getCurrentLocationAndPrintInfo(
    locationClient: FusedLocationProviderClient,
    context: Context,
    callback: (String, Double, Double) -> Unit
) {
    val permission =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val permission1 =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    var latitude: Double
    var longitude: Double

    if (permission == PackageManager.PERMISSION_GRANTED &&
        permission1 == PackageManager.PERMISSION_GRANTED) {


        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            0,
        ).setMinUpdateIntervalMillis(0).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locations: LocationResult) {
                for (location in locations.locations) {
                    latitude = location.latitude
                    longitude = location.longitude
                    callback.invoke(
                        "Latitude: ${latitude}, Longitude: ${longitude}",
                        latitude,
                        longitude
                    )
                }

            }
        }
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        locationClient.removeLocationUpdates(locationCallback)
        locationClient.lastLocation.addOnSuccessListener {
            latitude = it.latitude
            longitude = it.longitude
            callback.invoke(
                "Latitude: ${latitude}, Longitude: ${longitude}",
                latitude,
                longitude
            )
            return@addOnSuccessListener
        }.addOnFailureListener {
            callback.invoke(
                "Latitude: 0.0, Longitude: 0.0",
                0.0,
                0.0
            )
        }
    } else {
        callback.invoke("", 0.0, 0.0)
    }
}
/**
 * Função para enviar uma imagem para o armazenamento do Firebase e obter a URL de download.
 *
 * @param imageUri URI da imagem a ser enviada para o armazenamento do Firebase.
 * @param onComplete Callback que será chamado após o upload da imagem, fornecendo a URL de download.
 */
fun uploadImageToFirebaseStorage(imageUri: Uri, onComplete: (String) -> Unit) {
    val storage = Firebase.storage
    val storageRef = storage.reference
    val imagesRef = storageRef.child("images/${imageUri.lastPathSegment}")

    val uploadTask = imagesRef.putFile(imageUri)

    uploadTask.addOnSuccessListener { taskSnapshot ->
        imagesRef.downloadUrl.addOnSuccessListener { uri ->
            val downloadUrl = uri.toString()
            onComplete(downloadUrl)
        }.addOnFailureListener {
        }
    }.addOnFailureListener {
    }
}

/**
 * Composable que exibe um mapa do Google com um marcador na localização atual do dispositivo.
 *
 * @param locationClient Cliente FusedLocationProviderClient para obter a localização atual.
 * @param context Contexto atual da aplicação.
 */
@Composable
fun MapContent(locationClient: FusedLocationProviderClient, context: Context) {
    var latitudeM by remember { mutableStateOf(0.0) }
    var longitudeM by remember { mutableStateOf(0.0) }

    var locationObtained by remember { mutableStateOf(false) }

    getCurrentLocationAndPrintInfo(locationClient, context) { locationInfo, latitude, longitude ->
        latitudeM = latitude
        longitudeM = longitude
        locationObtained = true
    }

    if (locationObtained) {
        val markerLatLng = LatLng(latitudeM, longitudeM)
        val initialCameraPosition = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(markerLatLng, 15f)
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = initialCameraPosition
        ) {
            Marker(
                state = MarkerState(markerLatLng),
                title = "Localização Atual"
            )
        }
    }
}