package com.example.simplemusicplayer_lab7

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var songs: ArrayList<Song> = ArrayList()
    private var currentSongIndex = -1

    var loopMode = 0

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification("Музыкальный плеер готов")
        startForeground(1, notification)
    }

    fun setSongList(songList: ArrayList<Song>) {
        songs = songList
    }

    fun playSong(index: Int) {
        if (songs.isEmpty()) return

        currentSongIndex = index
        if (currentSongIndex >= songs.size) currentSongIndex = 0
        if (currentSongIndex < 0) currentSongIndex = songs.size - 1

        val song = songs[currentSongIndex]

        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
            setOnCompletionListener {
                onTrackFinished()
            }
        }

        val notif = createNotification("Играет: ${song.title}")
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notif)
    }

    private fun onTrackFinished() {
        if (loopMode == 1) {
            playSong(currentSongIndex)
        } else {
            playNext()
        }
    }

    fun playPause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
        }
    }

    fun playNext() {
        playSong(currentSongIndex + 1)
    }

    fun playPrev() {
        playSong(currentSongIndex - 1)
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentSongTitle(): String {
        if (currentSongIndex != -1 && currentSongIndex < songs.size) {
            return songs[currentSongIndex].title
        }
        return "Выберите песню"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "music_channel", "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "music_channel")
            .setContentTitle("Лабораторная №7")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}