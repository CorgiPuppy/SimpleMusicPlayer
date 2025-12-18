package com.example.simplemusicplayer

data class Song(val title: String, val path: String) {
    override fun toString(): String {
        return title
    }
}
