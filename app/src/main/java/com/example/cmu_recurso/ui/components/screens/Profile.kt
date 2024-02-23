package com.example.cmu_recurso.ui.components.screens

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cmu_recurso.viewmodel.ProfileViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * @param navController O controlador de navegação para navegar entre os composables.
 * Esta função representa a tela de autenticação do utilizador,
 * permitindo login, logout e exibição de mensagem de erro de conexão.
 */
@Composable
fun Profile(navController: NavController) {
    val context = LocalContext.current
    val email = rememberSaveable { mutableStateOf("") }
    val password = rememberSaveable { mutableStateOf("") }
    val viewModel: ProfileViewModel = viewModel()


    val user = Firebase.auth.currentUser
    if (user?.email != null) {
        email.value = user.email!!
        viewModel.authState.value = ProfileViewModel.AuthStatus.LOGGED
    }
    val connectivityManager =
        LocalContext.current.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isConnected = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    val authStatus = viewModel.authState.observeAsState()
    if (isConnected) {
        if (authStatus.value == ProfileViewModel.AuthStatus.NOLOGGIN) {
            LoginFields(
                email = email.value, password = password.value,
                onEmailChange = { email.value = it },
                onPasswordChange = { password.value = it },
                onLoginClick = { viewModel.login(email.value, password.value) },
                navController
            )
            return
        }
        if (authStatus.value == ProfileViewModel.AuthStatus.LOGGED) {
            LogoutFields {
                viewModel.logout()
            }
        }
    }else{
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "É necessário estar conectado à internet para se autenticar",
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
 * @param email O endereço de e-mail fornecido pelo utilizador.
 * @param password A senha fornecida pelo utilizador.
 * @param onEmailChange Callback para lidar com alterações no campo de e-mail.
 * @param onPasswordChange Callback para lidar com alterações no campo de senha.
 * @param onLoginClick Callback para lidar com o clique no botão de login.
 * @param navController O controlador de navegação para navegar para a tela de registo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginFields(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: (String) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Please login")
        TextField(
            value = email,
            placeholder = { Text(text = "user@email.com") },
            label = { Text(text = "email") },
            onValueChange = onEmailChange,
        )
        TextField(
            value = password,
            placeholder = { Text(text = "password") },
            label = { Text(text = "password") },
            onValueChange = onPasswordChange,
            visualTransformation = PasswordVisualTransformation()
        )

        Button(onClick = {
            if (email.isBlank() == false && password.isBlank() == false) {
                onLoginClick(email)
            } else {
                Toast.makeText(
                    context,
                    "Please enter an email and password",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }) {
            Text("Login")
        }
        Button(onClick = { navController.navigate("Register") }) {
            Text("Register")
        }
    }
}

/**
 * Esta função exibe o botão para realizar o logout do utilizador.
 * @param onLogoutClick Uma função de retorno de chamada para lidar com o clique no botão de logout.
 */
@Composable
fun LogoutFields(
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        verticalArrangement = Arrangement.spacedBy(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onLogoutClick) {
            Text("Logout!")
        }
    }
}


