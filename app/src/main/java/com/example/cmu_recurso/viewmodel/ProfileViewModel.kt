    package com.example.cmu_recurso.viewmodel

    import android.app.Application
    import android.util.Log
    import androidx.lifecycle.AndroidViewModel
    import androidx.lifecycle.MutableLiveData
    import androidx.lifecycle.viewModelScope
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.auth.ktx.auth
    import com.google.firebase.ktx.Firebase
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.tasks.await

    class ProfileViewModel(application:Application): AndroidViewModel(application) {

        val authState : MutableLiveData<AuthStatus>
        val fAuth : FirebaseAuth

        init {
            authState = MutableLiveData(AuthStatus.NOLOGGIN)
            fAuth = Firebase.auth
        }
        /**
         * Registra de um novo utilizador com o e-mail e password fornecidos.
         * @param email O endereço de e-mail do utilizador.
         * @param password A senha do utilizador.
         */
        fun register(email:String, password:String){
            viewModelScope.launch {
                try{
                    val result = fAuth.createUserWithEmailAndPassword(email, password).await()
                    if (result != null && result.user != null){
                        authState.postValue(AuthStatus.LOGGED)
                        Log.d("Register","logged in")
                        return@launch
                    }
                    Log.d("Register","anonymous")
                    authState.postValue(AuthStatus.NOLOGGIN)
                    return@launch
                } catch( e:Exception) {}
            }
        }
        /**
         * Realiza o login do utilizador com o e-mail e password fornecidos.
         * @param email O endereço de e-mail do utilizador.
         * @param password A senha do utilizador.
         */
        fun login(email:String, password:String){
            viewModelScope.launch {
                try{
                    val result = fAuth.signInWithEmailAndPassword(email, password).await()
                    if (result != null && result.user != null){
                        authState.postValue(AuthStatus.LOGGED)
                        Log.d("Login","logged in")
                        return@launch
                    }
                    Log.d("Login","anonymous")
                    authState.postValue(AuthStatus.NOLOGGIN)
                    return@launch
                } catch( e:Exception) {}
            }
        }
        /**
         * Realiza o logout do utilizador atualmente autenticado.
         */
        fun logout(){
            viewModelScope.launch {
                fAuth.signOut()
                authState.postValue(AuthStatus.NOLOGGIN)
                Log.d("Login","logout")
            }
        }

        enum class AuthStatus {
            LOGGED, NOLOGGIN
        }
    }
