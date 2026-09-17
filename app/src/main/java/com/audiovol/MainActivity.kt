package com.audiovol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.audiovol.databinding.ActivityMainBinding
import com.audiovol.other.SharedPref
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence

class MainActivity : AppCompatActivity() {
    private lateinit var audioManager: AudioManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPref: SharedPref

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != "android.media.VOLUME_CHANGED_ACTION") return

            val streamType = intent.getIntExtra(
                "android.media.EXTRA_VOLUME_STREAM_TYPE",
                -1
            )

            when (streamType) {
                AudioManager.STREAM_MUSIC -> setBarAndText(AudioManager.STREAM_MUSIC, binding.media.volumeBar, binding.media.volume)
                AudioManager.STREAM_RING -> setBarAndText(AudioManager.STREAM_RING, binding.phone.volumeBar, binding.phone.volume)
                AudioManager.STREAM_ALARM -> setBarAndText(AudioManager.STREAM_ALARM, binding.alarm.volumeBar, binding.alarm.volume)
            }
        }
    }

    private fun setupVolumeButton(
        view: View,
        onClick: () -> Unit,
        onLongPress: () -> Unit
    ) {

        val handler = Handler(Looper.getMainLooper())

        var isHolding = false

        val runnable = object : Runnable {
            override fun run() {

                if (isHolding) {
                    onLongPress()

                    handler.postDelayed(this, 100)
                }
            }
        }

        view.setOnClickListener {
            onClick()
        }

        view.setOnLongClickListener {
            isHolding = true

            // Start immediately
            onLongPress()

            // Continue repeatedly
            handler.postDelayed(runnable, 100)

            true
        }

        view.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {}

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    isHolding = false
                    handler.removeCallbacks(runnable)
                }
            }

            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        sharedPref = SharedPref(this)

        setContent()

        if(!sharedPref.getBoolean("tutorial",false)){
            setOnboarding()
        }

        setToggle()

        setDefaultVolume()

        controlVolume()

    }

    override fun onResume() {
        super.onResume()

        // Android 13+ requires an explicit exported/not-exported flag for
        // context-registered receivers. This broadcast only ever comes from
        // the system, so NOT_EXPORTED is correct here.
        ContextCompat.registerReceiver(
            this,
            volumeReceiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setDefaultVolume()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(volumeReceiver)
    }

    private fun setOnboarding() {

        TapTargetSequence(this@MainActivity)
            .continueOnCancel(true)
            .targets(
                TapTarget.forView(
                    binding.toggle,
                    "Volume Control",
                    "Tap to show the system volume panel.")
                    .outerCircleColor(R.color.yellow)
                    .targetCircleColor(R.color.red)
                    .titleTextColor(R.color.white)
                    .descriptionTextColor(R.color.white)
                    .transparentTarget(true),

                TapTarget.forView(
                    binding.media.minus,
                    "Decrease Volume",
                    "Tap/Hold to turn the volume down.")
                    .outerCircleColor(R.color.blue)
                    .targetCircleColor(R.color.white)
                    .titleTextColor(R.color.white)
                    .descriptionTextColor(R.color.white)
                    .transparentTarget(true),

                TapTarget.forView(
                    binding.media.plus,
                    "Increase Volume",
                    "Tap/Hold to turn the volume up.")
                    .outerCircleColor(R.color.red)
                    .targetCircleColor(R.color.yellow)
                    .titleTextColor(R.color.white)
                    .descriptionTextColor(R.color.white)
                    .transparentTarget(true),


                TapTarget.forView(
                    binding.media.volume,
                    "Volume Level",
                    "Shows your current volume as a percentage.")
                    .outerCircleColor(R.color.text_grey)
                    .targetCircleColor(R.color.blue)
                    .titleTextColor(R.color.white)
                    .descriptionTextColor(R.color.white)
                    .transparentTarget(true),
            )
            .listener(object : TapTargetSequence.Listener {

                override fun onSequenceFinish() {
                    sharedPref.putBoolean("tutorial", true)
                }

                override fun onSequenceStep(
                    lastTarget: TapTarget?,
                    targetClicked: Boolean) {}

                override fun onSequenceCanceled(lastTarget: TapTarget?) {}
            })
            .start()
    }
    private fun setDefaultVolume() {
        setBarAndText(AudioManager.STREAM_MUSIC, binding.media.volumeBar, binding.media.volume)
        setBarAndText(AudioManager.STREAM_RING, binding.phone.volumeBar, binding.phone.volume)
        setBarAndText(AudioManager.STREAM_ALARM, binding.alarm.volumeBar, binding.alarm.volume)
    }
    private fun setBarAndText(streamType: Int, bar: SegmentedBarView, volumeText: TextView) {
        val currentVolume = audioManager.getStreamVolume(streamType)
        val maxVolume = audioManager.getStreamMaxVolume(streamType)

        bar.setLevel(current = currentVolume, max = maxVolume)
        volumeText.text = "${getVolumePercentage(streamType)}%"
    }
    private fun getVolumePercentage(streamType: Int): Int {
        val currentVolume = audioManager.getStreamVolume(streamType)
        val maxVolume = audioManager.getStreamMaxVolume(streamType)

        return if (maxVolume > 0) {
            (currentVolume * 100) / maxVolume
        } else {
            0
        }
    }
    private fun setToggle() {

        binding.toggle.isChecked = if(sharedPref.getBoolean("check",false)) true else false

        binding.toggle.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                sharedPref.putBoolean("check",true)
            } else {
                sharedPref.putBoolean("check",false)
            }
        }
    }
//    private fun controlVolume() {
//
//        binding.media.plus.setOnClickListener {
//            increaseVolume(AudioManager.STREAM_MUSIC, binding.media.volume, binding.media.volumeBar)
//        }
//
//        binding.media.minus.setOnClickListener {
//            decreaseVolume(AudioManager.STREAM_MUSIC, binding.media.volume, binding.media.volumeBar)
//        }
//
//        binding.phone.plus.setOnClickListener {
//            increaseVolume(AudioManager.STREAM_RING, binding.phone.volume, binding.phone.volumeBar)
//        }
//
//        binding.phone.minus.setOnClickListener {
//            decreaseVolume(AudioManager.STREAM_RING, binding.phone.volume, binding.phone.volumeBar)
//        }
//
//        binding.alarm.plus.setOnClickListener {
//            increaseVolume(AudioManager.STREAM_ALARM, binding.alarm.volume, binding.alarm.volumeBar)
//        }
//
//        binding.alarm.minus.setOnClickListener {
//            decreaseVolume(AudioManager.STREAM_ALARM, binding.alarm.volume, binding.alarm.volumeBar)
//        }
//    }

    private fun controlVolume() {

        setupVolumeButton(
            binding.media.plus,
            onClick = {
                increaseVolume(
                    AudioManager.STREAM_MUSIC,
                    binding.media.volume,
                    binding.media.volumeBar
                )
            },
            onLongPress = {
                increaseVolume(
                    AudioManager.STREAM_MUSIC,
                    binding.media.volume,
                    binding.media.volumeBar
                )
            }
        )

        setupVolumeButton(
            binding.media.minus,
            onClick = {
                decreaseVolume(
                    AudioManager.STREAM_MUSIC,
                    binding.media.volume,
                    binding.media.volumeBar
                )
            },
            onLongPress = {
                decreaseVolume(
                    AudioManager.STREAM_MUSIC,
                    binding.media.volume,
                    binding.media.volumeBar
                )
            }
        )

        setupVolumeButton(
            binding.phone.plus,
            onClick = {
                increaseVolume(
                    AudioManager.STREAM_RING,
                    binding.phone.volume,
                    binding.phone.volumeBar
                )
            },
            onLongPress = {
                increaseVolume(
                    AudioManager.STREAM_RING,
                    binding.phone.volume,
                    binding.phone.volumeBar
                )
            }
        )

        setupVolumeButton(
            binding.phone.minus,
            onClick = {
                decreaseVolume(
                    AudioManager.STREAM_RING,
                    binding.phone.volume,
                    binding.phone.volumeBar
                )
            },
            onLongPress = {
                decreaseVolume(
                    AudioManager.STREAM_RING,
                    binding.phone.volume,
                    binding.phone.volumeBar
                )
            }
        )

        setupVolumeButton(
            binding.alarm.plus,
            onClick = {
                increaseVolume(
                    AudioManager.STREAM_ALARM,
                    binding.alarm.volume,
                    binding.alarm.volumeBar
                )
            },
            onLongPress = {
                increaseVolume(
                    AudioManager.STREAM_ALARM,
                    binding.alarm.volume,
                    binding.alarm.volumeBar
                )
            }
        )

        setupVolumeButton(
            binding.alarm.minus,
            onClick = {
                decreaseVolume(
                    AudioManager.STREAM_ALARM,
                    binding.alarm.volume,
                    binding.alarm.volumeBar
                )
            },
            onLongPress = {
                decreaseVolume(
                    AudioManager.STREAM_ALARM,
                    binding.alarm.volume,
                    binding.alarm.volumeBar
                )
            }
        )
    }
    private fun setContent() {

        binding.media.volumeBar.activeColor = ContextCompat.getColor(this, R.color.yellow)
        binding.phone.volumeBar.activeColor = ContextCompat.getColor(this, R.color.blue)
        binding.alarm.volumeBar.activeColor = ContextCompat.getColor(this, R.color.red)

        val packageInfo = packageManager.getPackageInfo(packageName, 0)

        val versionName = packageInfo.versionName

        binding.version.text = "AudioVol . v" + versionName

        val mediaDrawable = binding.media.icon.background as GradientDrawable
        val phoneDrawable = binding.phone.icon.background as GradientDrawable
        val alarmDrawable = binding.alarm.icon.background as GradientDrawable

        mediaDrawable.setColor(ContextCompat.getColor(this, R.color.fill_media))
        phoneDrawable.setColor(ContextCompat.getColor(this, R.color.fill_phone))
        alarmDrawable.setColor(ContextCompat.getColor(this, R.color.fill_alarm))

        binding.media.icon.setImageResource(R.drawable.media)
        binding.media.mediatext.text = "Media"
        binding.media.subtext.text = "Music, video, apps"

        binding.phone.icon.setImageResource(R.drawable.phone)
        binding.phone.mediatext.text = "Phone"
        binding.phone.subtext.text = "Calls, notifications"

        binding.alarm.icon.setImageResource(R.drawable.alarm)
        binding.alarm.mediatext.text = "Alarm"
        binding.alarm.subtext.text = "Wake alerts"

    }
    private fun updateVolumePercentage(streamType: Int, volumeText: TextView, bar: SegmentedBarView) {

        val currentVolume = audioManager.getStreamVolume(streamType)
        val maxVolume = audioManager.getStreamMaxVolume(streamType)

        val percentage = if (maxVolume > 0) {
            (currentVolume * 100) / maxVolume
        } else {
            0
        }

        volumeText.text = "$percentage%"
        bar.setLevel(current = currentVolume, max = maxVolume)
    }
    private fun decreaseVolume(streamType: Int, volumeText: TextView, bar: SegmentedBarView) {

        val flag = if (binding.toggle.isChecked) {
            AudioManager.FLAG_SHOW_UI
        } else {
            0
        }

        val currentVolume = audioManager.getStreamVolume(streamType)

        try {

            if (currentVolume > 0) {
                audioManager.setStreamVolume(
                    streamType,
                    currentVolume - 1,
                    flag
                )
            }

            updateVolumePercentage(streamType, volumeText, bar)
        }
        catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(this@MainActivity,"Your device may be in Silent or Do Not Disturb mode. Check your sound settings", Toast.LENGTH_SHORT).show()
        }
    }
    private fun increaseVolume(streamType: Int, volumeText: TextView, bar: SegmentedBarView) {

        val flag = if (binding.toggle.isChecked) {
            AudioManager.FLAG_SHOW_UI
        } else {
            0
        }

        val currentVolume = audioManager.getStreamVolume(streamType)
        val maxVolume = audioManager.getStreamMaxVolume(streamType)

        try {
            if (currentVolume < maxVolume ) {
                audioManager.setStreamVolume(
                    streamType,
                    currentVolume + 1,
                    flag
                )
            }

            updateVolumePercentage(streamType, volumeText, bar)
        }catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(
                this@MainActivity,
                "Your device may be in Silent or Do Not Disturb mode. Check your sound settings",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}