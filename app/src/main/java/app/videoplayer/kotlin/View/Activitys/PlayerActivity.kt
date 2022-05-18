package app.videoplayer.kotlin.View.Activitys

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.videoplayer.kotlin.Models.Video
import app.videoplayer.kotlin.R
import app.videoplayer.kotlin.Utils.DoubleClickListener
import app.videoplayer.kotlin.databinding.ActivityPlayerBinding
import app.videoplayer.kotlin.databinding.MoreFeaturesBinding
import app.videoplayer.kotlin.databinding.SpeedDialogBinding
import com.bumptech.glide.load.engine.Resource
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.lang.Math.abs
import java.text.DecimalFormat
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener,
    GestureDetector.OnGestureListener {

    lateinit var binding: ActivityPlayerBinding
    lateinit var runnable: Runnable
    private var isSubtitle: Boolean = true
    private lateinit var playPauseBtn: ImageButton
    private lateinit var fullScreenBtn: ImageView
    private lateinit var videoTitle: TextView
    private lateinit var gestureDetectorCompat: GestureDetectorCompat

    companion object {
        private var audioManager: AudioManager? = null
        private var timer: Timer? = null
        private lateinit var player: ExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = 1
        private var repeat: Boolean = false
        private var isFullscreen: Boolean = false
        private var isLocked: Boolean = false
        lateinit var trackSelector: DefaultTrackSelector
        private var speed: Float = 1.0f
        var pipStatus: Int = 0
        lateinit var dialog: Dialog
        var nowPlayingId: String = ""
        private var brightness: Int = 0
        private var volume: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setTheme(R.style.playerActivityTheme)
        setContentView(binding.root)

        videoTitle = findViewById(R.id.videoTitle)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        fullScreenBtn = findViewById(R.id.fullScreenBtn)

        gestureDetectorCompat = GestureDetectorCompat(this, this)

        //for immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }

        //for handling video file intent
        try {

            if (intent.data?.scheme.contentEquals("content")) {
                playerList = ArrayList()
                position = 0
                val cursor = contentResolver.query(
                    intent.data!!,
                    arrayOf(MediaStore.Video.Media.DATA),
                    null,
                    null,
                    null
                )
                cursor?.let {
                    it.moveToFirst()
                    val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val file = File(path)
                    val video = Video(
                        id = "",
                        title = file.name,
                        duration = 0L,
                        artUri = Uri.fromFile(file),
                        path = path,
                        size = "",
                        folderName = ""
                    )
                    playerList.add(video)
                    cursor.close()
                }
                createPlayer()
                initializeBinding()
            } else {
                initializeBinding()
                initializeLayout()
            }

        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeLayout() {


        when (intent.getStringExtra("class")) {
            "AllVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.videoList)
                createPlayer()
            }
            "FolderActivity" -> {
                playerList = ArrayList()
                playerList.addAll(FoldersActivity.currentFolderVideo)
            }
            "SearchedVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.searchList)
                createPlayer()
            }
            "NowPlaying" -> {
                speed = 1.0f
                videoTitle.text = playerList[position].title
                doubleTapEnable()
                videoTitle.isSelected = true
                playVideo()
                playInFullScreen(enable = isFullscreen)
                seekBarFeature()
            }
        }

        if (repeat) findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
        else findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
    }

    @SuppressLint("PrivateResource", "SetTextI18n")
    private fun initializeBinding() {

        /*findViewById<FrameLayout>(R.id.forwardFL).setOnClickListener(DoubleClickListener(callback = object :
            DoubleClickListener.Callback {
            override fun doubleClicked() {
                binding.playerView.showController()
                findViewById<ImageButton>(R.id.forwardBtn).visibility = View.VISIBLE
                player.seekTo(player.currentPosition + 10000)
                moreTime = 0
            }
        }))

        findViewById<FrameLayout>(R.id.rewindFL).setOnClickListener(DoubleClickListener(callback = object :
            DoubleClickListener.Callback {
            override fun doubleClicked() {
                binding.playerView.showController()
                findViewById<ImageButton>(R.id.rewindBtn).visibility = View.VISIBLE
                player.seekTo(player.currentPosition - 10000)
                moreTime = 0
            }
        }))*/

        val customDialog =
            LayoutInflater.from(this).inflate(R.layout.more_features, binding.root, false)
        val bindingMF = MoreFeaturesBinding.bind(customDialog)

        dialog = MaterialAlertDialogBuilder(this).setView(customDialog)
            .setOnCancelListener { playVideo() }
            .setBackground(ColorDrawable(0x803700B3.toInt()))
            .create()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            finish()
            player.release()
        }

        playPauseBtn.setOnClickListener {
            if (player.isPlaying) pauseVideo() else playVideo()
        }

        findViewById<ImageView>(R.id.nextBtn).setOnClickListener { nextPrevVideo() }
        findViewById<ImageView>(R.id.prevBtn).setOnClickListener { nextPrevVideo(isNext = false) }
        findViewById<ImageButton>(R.id.repeatBtn).setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            }
        }

        fullScreenBtn.setOnClickListener {
            if (isFullscreen) {
                isFullscreen = false
                playInFullScreen(enable = false)
            } else {
                isFullscreen = true
                playInFullScreen(enable = true)
            }
        }

        findViewById<ImageButton>(R.id.lockBtn).setOnClickListener {
            if (!isLocked) {
                //for hiding
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                findViewById<ImageButton>(R.id.lockBtn).setImageResource(R.drawable.ic_lock)
            } else {
                //for showing
                isLocked = false
                binding.playerView.useController = true
                findViewById<ImageButton>(R.id.lockBtn).setImageResource(R.drawable.ic_lock_open)
            }
        }

        findViewById<ImageView>(R.id.moreFeaturesBtn).setOnClickListener {
            pauseVideo()
            playVideo()
            dialog.show()

            val audioTrack = ArrayList<String>()
            for (i in 0 until player.currentTrackGroups.length) {
                if (player.currentTrackGroups.get(i)
                        .getFormat(0).selectionFlags == C.SELECTION_FLAG_DEFAULT
                ) {
                    audioTrack.add(
                        Locale(
                            player.currentTrackGroups.get(i).getFormat(0).language.toString()
                        ).displayLanguage
                    )
                }
            }

            val tempTracks = audioTrack.toArray(arrayOfNulls<CharSequence>(audioTrack.size))
            bindingMF.audioTrack.setOnClickListener {
                dialog.dismiss()
                MaterialAlertDialogBuilder(this, R.style.alertDialog)
                    .setTitle("Select Language")
                    .setOnCancelListener { playVideo() }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .setItems(tempTracks) { _, position ->
                        Toast.makeText(this, audioTrack[position] + "Selected", Toast.LENGTH_SHORT)
                            .show()
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setPreferredAudioLanguage(audioTrack[position])
                        )
                    }
                    .create().show()
            }

            bindingMF.subtitlesBtn.setOnClickListener {
                if (isSubtitle) {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, true
                        ).build()
                    Toast.makeText(this, "Subtitle Off", Toast.LENGTH_SHORT).show()
                    isSubtitle = false
                } else {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, false
                        ).build()
                    Toast.makeText(this, "Subtitle On", Toast.LENGTH_SHORT).show()
                    isSubtitle = true
                }
                dialog.dismiss()
                playVideo()
            }

            bindingMF.speedBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogS =
                    LayoutInflater.from(this).inflate(R.layout.speed_dialog, binding.root, false)
                val bindingS = SpeedDialogBinding.bind(customDialogS)
                val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->
                        playVideo()
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogS.show()

                bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                bindingS.minusBtn.setOnClickListener {
                    changeSpeed(isIncrement = false)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
                bindingS.plusBtn.setOnClickListener {
                    changeSpeed(isIncrement = true)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
            }

            bindingMF.sleepTimer.setOnClickListener {
                dialog.dismiss()
                if (timer != null) {
                    Toast.makeText(
                        this,
                        "Timer Already Running!!\n Close App to Rest Timer!!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    var sleepTime = 15
                    val customDialogS =
                        LayoutInflater.from(this)
                            .inflate(R.layout.speed_dialog, binding.root, false)
                    val bindingS = SpeedDialogBinding.bind(customDialogS)
                    val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                        .setCancelable(false)
                        .setPositiveButton("OK") { self, _ ->
                            timer = Timer()
                            val task = object : TimerTask() {
                                override fun run() {
                                    moveTaskToBack(true)
                                    exitProcess(1)
                                }
                            }
                            timer!!.schedule(task, sleepTime * 60 * 1000.toLong())
                            self.dismiss()
                            playVideo()
                        }
                        .setBackground(ColorDrawable(0x803700B3.toInt()))
                        .create()
                    dialogS.show()

                    bindingS.speedText.text = "$sleepTime Min"
                    bindingS.minusBtn.setOnClickListener {
                        if (sleepTime > 15) sleepTime -= 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                    bindingS.plusBtn.setOnClickListener {
                        if (sleepTime < 120) sleepTime -= 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                }
            }

            bindingMF.pipModeBtn.setOnClickListener {
                pictureInPictureMode(dialog)
            }
        }
    }

    private fun pictureInPictureMode(dialog: Dialog) {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(), packageName
            ) == AppOpsManager.MODE_ALLOWED
        } else false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (status) {
                this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                dialog.dismiss()
                binding.playerView.hideController()
                playVideo()
                pipStatus = 0
            } else {
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:${packageName}")
                )
                startActivity(intent)
            }
        } else {
            Toast.makeText(
                this,
                "Feature Not Supported!!",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
            playVideo()
        }
    }

    private fun createPlayer() {
        try {
            player.release()
        } catch (e: Exception) {
        }
        speed = 1.0f
        trackSelector = DefaultTrackSelector(this)
        videoTitle.text = playerList[position].title
        videoTitle.isSelected = true
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        doubleTapEnable()
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        playVideo()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) nextPrevVideo()
            }
        })
        playInFullScreen(enable = isFullscreen)
        //setVisibility()
        nowPlayingId = playerList[position].id

        seekBarFeature()
    }

    private fun playVideo() {
        playPauseBtn.setImageResource(R.drawable.ic_pause)
        player.play()
    }

    private fun pauseVideo() {
        playPauseBtn.setImageResource(R.drawable.ic_play)
        player.pause()
    }

    private fun nextPrevVideo(isNext: Boolean = true) {
        if (isNext) setPosition() else setPosition(isIncrement = false)
        createPlayer()
    }

    private fun setPosition(isIncrement: Boolean = true) {
        if (!repeat) {
            if (isIncrement)
                if (playerList.size - 1 == position) position = 0 else ++position
            else
                if (position == 0) position = playerList.size - 1 else --position
        }
    }

    private fun playInFullScreen(enable: Boolean) {
        if (enable) {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            fullScreenBtn.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenBtn.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    /* private fun setVisibility() {
         runnable = Runnable {
             if (binding.playerView.isControllerVisible) changeVisibility(View.VISIBLE)
             else changeVisibility(View.INVISIBLE)
             Handler(Looper.getMainLooper()).postDelayed(runnable, 0)
         }
         Handler(Looper.getMainLooper()).postDelayed(runnable, 0)
     }*/

    /* private fun changeVisibility(visibility: Int) {
         findViewById<LinearLayout>(R.id.topController).visibility = visibility
         findViewById<LinearLayout>(R.id.bottomController).visibility = visibility
         playPauseBtn.visibility = visibility
         findViewById<ImageButton>(R.id.lockBtn).visibility = visibility
         if (isLocked) findViewById<ImageButton>(R.id.lockBtn).visibility = View.VISIBLE
         else findViewById<ImageButton>(R.id.lockBtn).visibility = visibility
         if (moreTime == 2) {
             //findViewById<ImageButton>(R.id.rewindBtn).visibility = View.GONE
             //findViewById<ImageButton>(R.id.forwardBtn).visibility = View.GONE
         } else ++moreTime

         //for lockscreen -- hiding double tap
         //findViewById<FrameLayout>(R.id.rewindFL).visibility = visibility
         //findViewById<FrameLayout>(R.id.forwardFL).visibility = visibility

     }*/

    private fun changeSpeed(isIncrement: Boolean) {
        if (isIncrement) {
            if (speed < 2.9f) speed += 0.10f
        } else
            if (speed > 0.20f)
                speed -= 0.10f

        player.setPlaybackSpeed(speed)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        if (pipStatus != 0) {
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when (pipStatus) {
                1 -> intent.putExtra("class", "FolderActivity")
                2 -> intent.putExtra("class", "SearchedVideos")
                3 -> intent.putExtra("class", "AllVideos")
            }
            startActivity(intent)
        }

        if (!isInPictureInPictureMode) pauseVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.pause()
        audioManager?.abandonAudioFocus { this }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        if (audioManager == null) audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        if (brightness != 0) setScreenBrightness(brightness)

        playVideo()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode)
            pauseVideo()
        else playVideo()
    }

    private fun screenOrientation() {
        requestedOrientation =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doubleTapEnable() {
        binding.playerView.player = player
        binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.ytOverlay.visibility = View.VISIBLE
            }
        })
        binding.ytOverlay.player(player)
        binding.playerView.setOnTouchListener { _, motionEvent ->

            binding.playerView.isDoubleTapEnabled = false
            if (!isLocked) {
                binding.playerView.isDoubleTapEnabled = true
                gestureDetectorCompat.onTouchEvent(motionEvent)
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    binding.icBrightness.visibility = View.GONE
                    binding.icVolume.visibility = View.GONE
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun seekBarFeature() {
        findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(object :
            TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                pauseVideo()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                player.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                playVideo()
            }
        })
    }

    override fun onDown(p0: MotionEvent?): Boolean = false

    override fun onShowPress(p0: MotionEvent?) = Unit

    override fun onSingleTapUp(p0: MotionEvent?): Boolean = false

    override fun onLongPress(p0: MotionEvent?) = Unit

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean = false

    override fun onScroll(
        event: MotionEvent?,
        event1: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        val sWidth = Resources.getSystem().displayMetrics.widthPixels
        if (abs(distanceX) < abs(distanceY)) {
            if (event!!.x < sWidth / 2) {
                binding.icBrightness.visibility = View.VISIBLE
                binding.icVolume.visibility = View.GONE
                val increase = distanceY > 0
                val newValue = if (increase) brightness + 1 else brightness - 1
                if (newValue in 0..30) brightness = newValue
                binding.icBrightness.text = brightness.toString()
                setScreenBrightness(brightness)
            } else {
                binding.icBrightness.visibility = View.GONE
                binding.icVolume.visibility = View.VISIBLE

                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue = if (increase) volume + 1 else volume - 1
                if (newValue in 0..30) volume = newValue
                binding.icBrightness.text = volume.toString()
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }
        }
        return true
    }


    private fun setScreenBrightness(value: Int) {
        val d = 1.0f / 30
        val lp = this.window.attributes
        lp.screenBrightness = d * value
        this.window.attributes = lp
    }

}