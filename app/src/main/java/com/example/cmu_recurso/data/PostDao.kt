package com.example.cmu_recurso.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PostDao {
    @Query("SELECT * FROM post")
    fun getPosts(): LiveData<List<Post>>

    @Query("select * from post where id = :id")
    fun getOne(id:Int):LiveData<Post>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post)

    @Update
    suspend fun update(post: Post)

    @Delete
    suspend fun delete(post: Post)

}