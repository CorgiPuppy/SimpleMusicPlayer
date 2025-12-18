package com.example.simplemusicplayer

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

    var onSongChangedListener: (() -> Unit)? = null

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
        val notification = createNotification(getString(R.string.notification_ready))
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

        onSongChangedListener?.invoke()

        val notifText = getString(R.string.notification_playing_template, song.title)
        val notif = createNotification(notifText)

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
            onSongChangedListener?.invoke()
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
        return getString(R.string.select_song)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val channel = NotificationChannel(
                "music_channel", name,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val title = getString(R.string.notification_title)
        return NotificationCompat.Builder(this, "music_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}