package com.example.cmu_recurso.data

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromCommentList(comments: List<Comment>?): String {
        return Gson().toJson(comments)
    }

    @TypeConverter
    @JvmStatic
    fun toCommentList(commentsString: String): List<Comment> {
        val listType = object : TypeToken<List<Comment>>() {}.type
        return Gson().fromJson(commentsString, listType)
    }
    @TypeConverter
    @JvmStatic
    fun fromRatingList(ratings: List<Rating>?): String {
        return Gson().toJson(ratings)
    }

    @TypeConverter
    @JvmStatic
    fun toRatingList(ratingsString: String): List<Rating> {
        val listType = object : TypeToken<List<Rating>>() {}.type
        return Gson().fromJson(ratingsString, listType)
    }
}