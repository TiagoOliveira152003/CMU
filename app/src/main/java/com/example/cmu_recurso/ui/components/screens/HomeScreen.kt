package com.example.cmu_recurso.ui.components.screens


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.preference.PreferenceManager
import android.widget.RatingBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmu_recurso.R
import com.example.cmu_recurso.data.Category
import com.example.cmu_recurso.data.Comment
import com.example.cmu_recurso.data.Post
import com.example.cmu_recurso.data.Rating
import com.example.cmu_recurso.viewmodel.PostViewModel
import com.google.accompanist.coil.rememberCoilPainter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun HomeScreen() {
    Teste()
}
/**
 * Função para extrair as coordenadas (latitude e longitude) de uma string formatada.
 *
 * @param coordinatesString String que contém as coordenadas no formato "Latitude: {latitude}, Longitude: {longitude}".
 * @return Um par de valores que representa a latitude e longitude, ou null se a string não estiver no formato esperado.
 */
fun extractCoordinates(coordinatesString: String?): Pair<Double, Double>? {
    val parts = coordinatesString?.split(",")

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    if (parts != null) {
        for (part in parts) {
            val components = part.trim().split(" ")

            if (components.size != 2) {
                return null
            }

            when (components[0]) {
                "Latitude:" -> latitude = components[1].toDoubleOrNull() ?: return null
                "Longitude:" -> longitude = components[1].toDoubleOrNull() ?: return null
            }
        }
    }

    return Pair(latitude, longitude)
}

/**
 * Tela inicial que executará todas as outras funções
 */
@Composable
fun Teste() {
    var notificationId = 0
    val ctx = LocalContext.current
    val postsViewModel: PostViewModel = viewModel()
    val user = Firebase.auth.currentUser
    val isUserLoggedIn = user?.email != null
    val books = postsViewModel.listPosts.observeAsState()
    val locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)
    val connectivityManager =
        LocalContext.current.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isConnected = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

        books.value?.forEach {
            getCurrentLocationAndPrintInfo(
                locationClient,
                ctx,

                ) { str, lat, lon ->
                it.coordinates?.let { coordinatesString ->
                    extractCoordinates(coordinatesString)?.let { (latitude, longitude) ->
                        val eventLocation = LatLng(latitude, longitude)
                        val distance = calculateDistance(
                            lat,
                            lon,
                            eventLocation.latitude,
                            eventLocation.longitude
                        )

                        if (distance <= 100) {
                            sendNotification(
                                ctx,
                                "Acontecimento próximo em: ${it.address}",
                                "${it.category} , ${it.title} , ${it.description}",
                                notificationId
                            )
                            notificationId += 1
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isUserPost = user?.email == it.userEmail
                if (isConnected) {
                    if (isUserPost) {
                        IconButton(
                            onClick = {
                                postsViewModel.deletePost(it)
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_message_24),
                                contentDescription = "Eliminar post"
                            )
                        }
                    }
                    SharePostScreen("Acontecimento próximo em: ${it.address}, ${it.category} , ${it.title} , ${it.description}")
                }
            }
            DisplayPost(postsViewModel, post = it) { comment ->
                postsViewModel.addCommentToPost(it, comment)
            }
            Divider(
                thickness = 2.dp
            )
        }
    }
}

/**
 * Função composable usada para mostrar um post
 *
 * @param postViewModel O ViewModel utilizado para gerenciar os posts.
 * @param post O objeto Post a ser exibido.
 * @param onCommentSubmit Uma função de retorno de chamada para submeter um novo comentário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayPost(postViewModel: PostViewModel, post: Post, onCommentSubmit: (Comment) -> Unit) {
    var areCommentsExpanded by remember { mutableStateOf(false) }
    var isUpvoted by rememberSaveable { mutableStateOf(false) }
    var isDownvoted by rememberSaveable { mutableStateOf(false) }
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    val upvotedKey = "upvoted_${post.id}"
    val downvotedKey = "downvoted_${post.id}"
    isUpvoted = sharedPreferences.getBoolean(upvotedKey, false)
    isDownvoted = sharedPreferences.getBoolean(downvotedKey, false)
    val connectivityManager =
        LocalContext.current.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isConnected = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Título: ${post.title}")
            Divider()
            Text(text = "Descrição: ${post.description}")
            Divider()
            Text(text = "Categoria: ${post.category}")
            Divider()
            Text(text = "Local: ${post.address}")
            Divider()
            if (post.category != Category.CURIOSIDADES) {
                Text(text = "Duração: ${post.lifespan} dias")
                Divider()
            }
            Text(text = "Criado por: ${post.userEmail}")
            Divider()
            Text(text = "Upvotes: ${post.upvotes}")
            Divider()
            Text(text = "Downvotes: ${post.downvotes}")
            Divider()
        }
        if (post.photoUrl != "") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                ) {
                    post.photoUrl?.let { url ->
                        Image(
                            painter = rememberCoilPainter(request = url),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        val user = Firebase.auth.currentUser
        var userRating by rememberSaveable { mutableStateOf(0f) }
        var avgRating by rememberSaveable { mutableStateOf(0.0) }

        val existingRatingIndex = post.userRatings.indexOfFirst { it.user == user?.email ?: "" }
        if (existingRatingIndex != -1) {
            userRating = post.userRatings[existingRatingIndex].rating.toFloat()
        }
        /**
        * Função para calcular a média das avaliações de um post.
        */
        fun calculateRatingAverage(): Double {
            if (post.userRatings.isEmpty()) {
                return 0.0
            }

            var totalRating = 0.0
            post.userRatings.forEach { rating ->
                totalRating += rating.rating
            }

            return totalRating / post.userRatings.size
        }

        avgRating = calculateRatingAverage()

        if (user!=null && isConnected) {
            val onUserRatingChanged: (Float) -> Unit = { newRating ->
                if (user != null) {
                    val userEmail = user.email
                    if (userEmail != null) {
                        val existingRatingIndex =
                            post.userRatings.indexOfFirst { it.user == userEmail }
                        if (existingRatingIndex != -1) {
                            post.userRatings[existingRatingIndex] =
                                Rating(userEmail, newRating.toDouble())
                        } else {
                            post.userRatings.add(Rating(userEmail, newRating.toDouble()))
                        }
                        user.email?.let {
                            postViewModel.updateUserRating(
                                post
                            )
                        }
                    }
                }
                avgRating = calculateRatingAverage()
            }
            CustomRatingBar(
                rating = userRating,
                onRatingChanged = onUserRatingChanged
            )
        }
        Text(
            text = "Classificação Média: $avgRating",
            modifier = Modifier
                .padding(vertical = 8.dp)
                .align(CenterHorizontally)
        )
        val isUserLoggedIn = user?.email != null
        var commentText by rememberSaveable { mutableStateOf("") }
        /**
         * Função auxiliar para verificar se o utilizador votou num post.
         */
        fun isUserVoted(): Boolean {
            val userEmail = user?.email ?: return false
            return post.upvotedBy.contains(userEmail) || post.downvotedBy.contains(userEmail)
        }

        val userVoted = isUserVoted()
        isUpvoted = userVoted && post.upvotedBy.contains(user?.email)
        isDownvoted = userVoted && post.downvotedBy.contains(user?.email)
        if (isConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (!isUpvoted) {
                            if (isDownvoted) {
                                post.downvotes -= 1
                                if (user != null && post.downvotedBy.contains(user.email)) {
                                    post.downvotedBy.remove(user.email)
                                }
                                if (user != null) {
                                    user.email?.let {
                                        postViewModel.updateDownvotesRemove(
                                            post,
                                            it
                                        )
                                    }
                                }
                                isDownvoted = false
                                with(sharedPreferences.edit()) {
                                    putBoolean(downvotedKey, false)
                                    apply()
                                }
                            }
                            post.upvotes += 1
                            if (user != null) {
                                user.email?.let { postViewModel.updateUpvotesAdd(post, it) }
                            }
                            isUpvoted = true
                            with(sharedPreferences.edit()) {
                                putBoolean(upvotedKey, true)
                                apply()
                            }
                        }
                    },
                    enabled = user != null && !isUpvoted
                ) {
                    Text(text = "Upvote")
                }
                Button(
                    onClick = {
                        if (!isDownvoted) {
                            if (isUpvoted) {
                                post.upvotes -= 1
                                if (user != null && post.upvotedBy.contains(user.email)) {
                                    post.upvotedBy.remove(user.email)
                                }
                                if (user != null) {
                                    user.email?.let { postViewModel.updateUpvotesRemove(post, it) }
                                }
                                isUpvoted = false
                                with(sharedPreferences.edit()) {
                                    putBoolean(upvotedKey, false)
                                    apply()
                                }
                            }
                            post.downvotes += 1
                            if (user != null) {
                                user.email?.let { postViewModel.updateDownvotesAdd(post, it) }
                            }
                            isDownvoted = true
                            with(sharedPreferences.edit()) {
                                putBoolean(downvotedKey, true)
                                apply()
                            }

                        }
                    },
                    enabled = user != null && !isDownvoted
                ) {
                    Text(text = "Downvote")
                }
                if (user != null && (isUpvoted || isDownvoted)) {
                    Button(
                        onClick = {
                            user?.email?.let { email ->
                                if (isUpvoted) {
                                    if (post.upvotedBy.contains(email)) {
                                        post.upvotedBy.remove(email)
                                        post.upvotes -= 1
                                        postViewModel.updateUpvotesRemove(post, email)
                                    }
                                } else if (isDownvoted) {
                                    if (post.downvotedBy.contains(email)) {
                                        post.downvotedBy.remove(email)
                                        post.downvotes -= 1
                                        postViewModel.updateDownvotesRemove(post, email)
                                    }
                                }
                                isDownvoted = false
                                isUpvoted = false
                                with(sharedPreferences.edit()) {
                                    putBoolean(upvotedKey, false)
                                    putBoolean(downvotedKey, false)
                                    apply()
                                }
                            }
                        }
                    ) {
                        Text(text = "Reset Votes")
                    }
                }
            }
        }
        if (isConnected) {

            if (isUserLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Comentário") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                val comment = Comment(
                                    user = Firebase.auth.currentUser?.email.toString(),
                                    text = commentText
                                )
                                postViewModel.addCommentToPost(post,comment)
                                onCommentSubmit(comment)
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = "+", fontSize = 24.sp
                        )

                    }
                }
            }
            Button(
                onClick = { areCommentsExpanded = !areCommentsExpanded },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(text = if (areCommentsExpanded) "Recolher Comentários" else "Expandir Comentários")
            }
            if (areCommentsExpanded) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (post.comments.size != 0) {
                        post.comments.forEach { comment ->
                            Text(text = "User: ${comment.user}")
                            Text(text = "Comentário: ${comment.text}")
                            Divider()
                        }
                    } else {
                        Text(text = "Não há comentários para exibir")
                    }
                }
            }
        }
    }
}
/**
 * Função composable que mostra uma RatingBar com propriedades costumizadas
 *
 * @param rating The current rating value.
 * @param onRatingChanged Callback function triggered when the rating changes.
 */
@Composable
fun CustomRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.align(Alignment.Center),
            factory = { context ->
                RatingBar(context).apply {
                    numStars = 5
                    stepSize = 0.5f
                    this.rating = rating
                    setOnRatingBarChangeListener { _, newRating, _ ->
                        onRatingChanged(newRating)
                    }
                }
            }
        ) { ratingBar ->
        }
    }
}

/**
 * Calcula a distância em metros entre duas coordenadas geográficas (latitude e longitude).
 *
 * @param lat1 Latitude da primeira coordenada.
 * @param lon1 Longitude da primeira coordenada.
 * @param lat2 Latitude da segunda coordenada.
 * @param lon2 Longitude da segunda coordenada.
 * @return A distância entre as coordenadas em metros.
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}
/**
 * Envia uma notificação para o dispositivo.
 *
 * @param context Contexto atual da aplicação.
 * @param title Título da notificação.
 * @param message Mensagem da notificação.
 * @param notificationId ID da notificação.
 */
private fun sendNotification(
    context: Context,
    title: String,
    message: String,
    notificationId: Int
) {
    val channelId = "default_channel_id"
    val channelName = "Default Channel"
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val notificationManager = NotificationManagerCompat.from(context)

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Default Channel Description"
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
            vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        }
        notificationManager.createNotificationChannel(notificationChannel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification_icon)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    notificationManager.notify(notificationId, builder.build())
}

/**
 * Função que exibe a tela para partilhar o conteúdo de um post.
 *
 * @param postContent Conteúdo do post a ser compartilhado.
 */
@Composable
fun SharePostScreen(postContent: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(
            onClick = {
                sharePostViaSMS(context, postContent)
            },
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Icon(
                painter = painterResource(id = androidx.appcompat.R.drawable.abc_ic_menu_share_mtrl_alpha),
                contentDescription = "Partilhar post",
                tint = androidx.compose.ui.graphics.Color.Black
            )
        }
    }
}

/**
 * Função que compartilha o conteúdo de um post via SMS.
 *
 * @param context Contexto atual da aplicação.
 * @param content Conteúdo a ser compartilhado via SMS.
 */
fun sharePostViaSMS(context: Context, content: String) {
    val sendIntent = Intent(Intent.ACTION_VIEW)
    sendIntent.data = Uri.parse("sms:")
    sendIntent.putExtra("sms_body", content)

    if (sendIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(sendIntent)
    }
}
