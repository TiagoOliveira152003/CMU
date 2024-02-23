package com.example.cmu_recurso.repository

import androidx.lifecycle.LiveData
import com.example.cmu_recurso.data.Post
import com.example.cmu_recurso.data.PostDao

class PostRepository(val postDao: PostDao) {
    fun getPosts(): LiveData<List<Post>> {
        return postDao.getPosts()
    }

    fun getPost(id:Int): LiveData<Post> {
        return postDao.getOne(id)
    }


    suspend fun insert(book:Post){
        postDao.insert(book)
    }

    suspend fun update(book:Post){
        postDao.update(book)
    }


    suspend fun delete(post: Post){
        postDao.delete(post)
    }
    suspend fun updatePostInRoom(post: Post) {
        postDao.update(post)
    }
}