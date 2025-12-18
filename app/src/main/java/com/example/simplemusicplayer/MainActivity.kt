package com.example.simplemusicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var songListView: ListView
    private lateinit var currentSongTitle: TextView
    private lateinit var btnPlayPause: Button
    private lateinit var btnLoop: Button

    private var musicService: MusicService? = null
    private var isBound = false
    private val songList = ArrayList<Song>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicService?.setSongList(songList)
            isBound = true

            musicService?.onSongChangedListener = {
                updateUI()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        songListView = findViewById(R.id.songListView)
        currentSongTitle = findViewById(R.id.currentSongTitle)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnLoop = findViewById(R.id.btnLoop)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnPrev = findViewById<Button>(R.id.btnPrev)

        checkPermissions()

        songListView.setOnItemClickListener { _, _, position, _ ->
            musicService?.playSong(position)
        }

        btnPlayPause.setOnClickListener {
            musicService?.playPause()
        }

        btnNext.setOnClickListener {
            musicService?.playNext()
        }

        btnPrev.setOnClickListener {
            musicService?.playPrev()
        }

        btnLoop.setOnClickListener {
            if (musicService != null) {
                if (musicService!!.loopMode == 0) {
                    musicService!!.loopMode = 1
                    btnLoop.text = getString(R.string.btn_loop_one)
                } else {
                    musicService!!.loopMode = 0
                    btnLoop.text = getString(R.string.btn_loop_all)
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissions.add(Manifest.permission.READ_PHONE_STATE)

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 123)
        } else {
            loadSongs()
            startMusicService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
            startMusicService()
        } else {
            Toast.makeText(this, getString(R.string.permission_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSongs() {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = contentResolver.query(uri, projection, selection, null, null)

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val title = cursor.getString(0)
                val path = cursor.getString(1)
                songList.add(Song(title, path))
            }
            cursor.close()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songList)
        songListView.adapter = adapter
    }

    private fun startMusicService() {
        val intent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun updateUI() {
        runOnUiThread {
            currentSongTitle.text = musicService?.getCurrentSongTitle()

            if (musicService?.isPlaying() == true) {
                btnPlayPause.text = getString(R.string.btn_pause)
            } else {
                btnPlayPause.text = getString(R.string.btn_play)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            musicService?.onSongChangedListener = null
            unbindService(connection)
            isBound = false
        }
    }
}