package com.example.cmu_recurso.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cmu_recurso.data.Category
import com.example.cmu_recurso.data.Comment
import com.example.cmu_recurso.data.Post
import com.example.cmu_recurso.data.PostDatabase
import com.example.cmu_recurso.data.Rating
import com.example.cmu_recurso.repository.PostRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostViewModel(application: Application) : AndroidViewModel(application) {
    val repository: PostRepository
    val allPosts: LiveData<List<Post>>

    val dbf: FirebaseFirestore
    val collectionName: String
    val listPosts: MutableLiveData<List<Post>>

    init {
        dbf = Firebase.firestore
        collectionName = "POSTS"
        listPosts = MutableLiveData(listOf())
        val db = PostDatabase.getDatabase(application)
        repository = PostRepository(db.postDao())
        allPosts = repository.getPosts()
        syncPosts()
        getPostsLive()
    }
    /**
     * Insere um post no ROOM e na Firebase.
     * @param post O post a ser inserido.
     */
    fun insertPost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val timestampString = timestamp.toString()
            val updatedPost = post
            updatedPost.id = timestampString.toLong()

            repository.insert(updatedPost)

            savePost(updatedPost)
        }
    }
    /**
     * Atualiza um post no ROOM.
     * @param post O post a ser atualizado.
     */
    fun updatePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(post)
        }
    }
    /**
     * Obtém um único post do ROOM.
     * @param id O ID do post a ser obtido.
     * @return Um LiveData que contém o post.
     */
    fun getOnePost(id: Int): LiveData<Post> {
        return repository.getPost(id)
    }
    /**
     * Exclui um post do ROOM e do Firebase.
     * @param post O post a ser excluído.
     */
    fun deletePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(post)
            deletePostFromFirebase(post.id)
        }
    }
    /**
     * Guarda um post na Firebase.
     * @param post O post a ser guardado.
     */
    fun savePost(post: Post) {
        viewModelScope.launch {
            val commentsList = mutableListOf<Map<String, Any>>()
            post.comments.forEach { comment ->
                val commentMap = mapOf(
                    "user" to comment.user,
                    "text" to comment.text
                )
                commentsList.add(commentMap)
            }
            val ratingsList = mutableListOf<Map<String, Any>>()
            post.userRatings.forEach { rating ->
                val ratingMap = mapOf(
                    "user" to rating.user,
                    "rating" to rating.rating
                )
                ratingsList.add(ratingMap)
            }
            val postToSave = hashMapOf(
                "title" to post.title,
                "description" to post.description,
                "photoUrl" to post.photoUrl,
                "address" to post.address,
                "coordinates" to post.coordinates,
                "lifespan" to post.lifespan,
                "category" to post.category.name, // Save category as a string
                "upvotes" to post.upvotes,
                "downvotes" to post.downvotes,
                "userEmail" to post.userEmail,
                "comments" to commentsList,
                "upvotedBy" to post.upvotedBy,
                "downvotedby" to post.downvotedBy,
                "ratings" to ratingsList
            )


            dbf.collection(collectionName).document(post.id.toString()).set(post)
        }
    }
    /**
     * Exclui um post do Firebase.
     * @param postId O ID do post a ser excluído.
     */
    fun deletePostFromFirebase(postId: Long) {
        val postRef = dbf.collection(collectionName).document(postId.toString())
        postRef.delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Post deleted from Firebase: $postId")
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to delete post from Firebase: $postId", it)
            }
    }
    /**
     * Obtém posts em tempo real do Firebase.
     */
    fun getPostsLive() {
        viewModelScope.launch {
            val ref = dbf.collection(collectionName)
            ref.addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    val list = mutableListOf<Post>()
                    for (document in snapshot.documents) {
                        val postId = document.id
                        val post = document.toPost(postId)
                        post?.let {
                            list.add(it)

                        }
                    }
                    listPosts.postValue(list)
                }
            }
        }
    }
    /**
     * Converte um objeto da Firebase num objeto Post.
     * @param firebaseId O ID do documento no Firebase.
     * @return O objeto Post correspondente, ou null se a conversão falhar.
     */
    fun DocumentSnapshot.toPost(firebaseId: String): Post? {
        return try {
            val id = firebaseId.toLong()
            val title = getString("title") ?: ""
            val description = getString("description") ?: ""
            val photoUrl = getString("photoUrl") ?: ""
            val address = getString("address") ?: ""
            val coordinates = getString("coordinates") ?: ""
            val lifespan = getLong("lifespan") ?: 0
            val state = getBoolean("state") ?: false
            val categoryStr = getString("category") ?: ""
            val category = Category.valueOf(categoryStr.toUpperCase())
            val upvotes = getLong("upvotes")?.toInt() ?: 0
            val downvotes = getLong("downvotes")?.toInt() ?: 0
            val userEmail = getString("userEmail") ?: ""

            val comments = arrayListOf<Comment>() // Inicializa uma lista de comentários vazia
            val upvotedBy = arrayListOf<String>()
            val downvotedBy = arrayListOf<String>()
            val ratings = arrayListOf<Rating>() // Inicializa uma lista de comentários vazia

            val commentsList = get("comments") as? List<Map<String, Any>>?
            commentsList?.forEach { commentMap ->
                val user = commentMap["user"] as? String ?: ""
                val text = commentMap["text"] as? String ?: ""
                comments.add(Comment(user, text))
            }
            val ratingsList = get("userRatings") as? List<Map<String, Any>>?
            ratingsList?.forEach { ratingmap ->
                val user = ratingmap["user"] as? String ?: ""
                val text = ratingmap["rating"] as? Double ?: 0.0
                ratings.add(Rating(user, text))
            }
            val upvotedList = get("upvotedBy") as? List<String>?
            upvotedList?.forEach { commentMap ->
                upvotedBy.add(commentMap)
            }
            val downvotedList = get("downvotedBy") as? List<String>?
            downvotedList?.forEach { commentMap ->
                downvotedBy.add(commentMap)
            }
            Post(
                title,
                description,
                photoUrl,
                address,
                coordinates,
                lifespan,
                category,
                upvotes,
                downvotes,
                userEmail,
                comments,
                upvotedBy,
                downvotedBy,
                ratings
            ).apply {
                this.id = id
            }
        } catch (e: Exception) {
            null
        }
    }
    /**
     * Sincroniza a lista de posts da Firebase com a lista de posts do ROOM.
     */
    fun syncPosts() {
        viewModelScope.launch {
            val allPostsList = allPosts.value.orEmpty()
            val listPostsList = listPosts.value.orEmpty().toMutableList()

            allPostsList.forEach { post ->
                if (!listPostsList.contains(post)) {
                    listPostsList.add(post)
                }
            }
            listPosts.value = listPostsList.toList()
        }
    }
    /**
     * Adiciona um comentário a um post.
     * @param post O post ao qual o comentário será adicionado.
     * @param comment O comentário a ser adicionado.
     */
    fun addCommentToPost(post: Post, comment: Comment) {
        viewModelScope.launch {
            val updatedComments = post.comments.toMutableList()
            updatedComments.add(comment)
            val updatedPost = post.copy(comments = updatedComments)
            updatedPost.id = post.id
            repository.updatePostInRoom(updatedPost)
            dbf.collection(collectionName).document(post.id.toString()).set(updatedPost)
        }
    }
    /**
     * Atualiza os votos positivos de um post.
     * @param post O post cujos votos serão atualizados.
     * @param email O e-mail do utilizador que vota.
     */
    fun updateUpvotesAdd(post: Post, email: String) {
        viewModelScope.launch {
            post.downvotedBy.removeIf { it.isBlank() }
            val updatedComments = post.upvotedBy
            updatedComments.add(email)
            updatedComments.removeIf{it.isBlank()}
            val updatedPost = post.copy(upvotedBy = updatedComments)
            updatedPost.id = post.id
            repository.updatePostInRoom(updatedPost)
            dbf.collection(collectionName).document(post.id.toString()).set(updatedPost)
        }
    }
    /**
     * Atualiza os votos positivos de um post.
     * @param post O post cujos votos serão atualizados.
     * @param email O e-mail do utilizador que vota.
     */
    fun updateUpvotesRemove(post: Post, email: String) {
        viewModelScope.launch {
            post.downvotedBy.removeIf { it.isBlank() }
            val updatedComments = post.upvotedBy
            updatedComments.remove(email)
            updatedComments.removeIf{it.isBlank()}
            val updatedPost = post.copy(upvotedBy = updatedComments)
            updatedPost.id = post.id
            repository.updatePostInRoom(updatedPost)
            dbf.collection(collectionName).document(post.id.toString()).set(updatedPost)
        }
    }
    /**
     * Atualiza os votos negativos de um post.
     * @param post O post cujos votos serão atualizados.
     * @param email O e-mail do utilizador que vota.
     */
    fun updateDownvotesAdd(post: Post, email: String) {
        viewModelScope.launch {
            post.upvotedBy.removeIf { it.isBlank() }
            val updatedComments = post.downvotedBy
            updatedComments.add(email)
            updatedComments.removeIf{it.isBlank()}
            val updatedPost = post.copy(downvotedBy = updatedComments)
            updatedPost.id = post.id
            repository.updatePostInRoom(updatedPost)
            dbf.collection(collectionName).document(post.id.toString()).set(updatedPost)
        }
    }
    /**
     * Atualiza os votos negativos de um post.
     * @param post O post cujos votos serão atualizados.
     * @param email O e-mail do utilizador que vota.
     */
    fun updateDownvotesRemove(post: Post, email: String) {
        viewModelScope.launch {
            post.upvotedBy.removeIf { it.isBlank() }
            val updatedComments = post.downvotedBy
            updatedComments.remove(email)
            updatedComments.removeIf{it.isBlank()}
            val updatedPost = post.copy(downvotedBy = updatedComments)
            updatedPost.id = post.id
            repository.updatePostInRoom(updatedPost)
            dbf.collection(collectionName).document(post.id.toString()).set(updatedPost)
        }
    }
    /**
     * Atualiza as classificações do utilizador atribuídas a um post.
     * @param post O post cujas classificações do utilizador serão atualizadas.
     */
    fun updateUserRating(post: Post) {
        viewModelScope.launch {
            val updatedRating = post.userRatings
            val updatedPost = post.copy(userRatings = updatedRating)
            updatedPost.id = post.id
            repository.updatePostInRoom(updatedPost)
            dbf.collection(collectionName).document(post.id.toString()).set(updatedPost)
        }
    }
}
