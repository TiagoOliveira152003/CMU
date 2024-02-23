package com.example.cmu_recurso.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Post(
    val title: String,
    val description: String,
    val photoUrl: String,
    val address: String,
    val coordinates: String,
    val lifespan: Long,
    val category: Category,
    var upvotes: Int = 0,
    var downvotes: Int = 0,
    val userEmail: String,
    var comments: List<Comment> = mutableListOf(),
    val upvotedBy: MutableList<String> = mutableListOf(),
    val downvotedBy: MutableList<String> = mutableListOf(),
    var userRatings: MutableList<Rating> = mutableListOf()
){
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
data class Comment(
    val user: String,
    val text: String
)
data class Rating(
    val user: String,
    val rating: Double
)

enum class Category { PERIGO, CURIOSIDADES, EVENTOS }
