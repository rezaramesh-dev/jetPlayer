package app.videoplayer.kotlin.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.videoplayer.kotlin.Models.Video
import app.videoplayer.kotlin.R
import app.videoplayer.kotlin.View.Activitys.MainActivity
import app.videoplayer.kotlin.View.Activitys.PlayerActivity
import app.videoplayer.kotlin.databinding.RenameFieldBinding
import app.videoplayer.kotlin.databinding.VideoMoreFeaturesBinding
import app.videoplayer.kotlin.databinding.VideoViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URI

class VideoAdapter(
    private val context: Context,
    private var videoList: ArrayList<Video>,
    private val isFolder: Boolean = false
) :
    RecyclerView.Adapter<VideoAdapter.MyHolder>() {

    class MyHolder(binding: VideoViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val folder = binding.folderName
        val duration = binding.duration
        val image = binding.videoImg
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(VideoViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.title.text = videoList[position].title
        holder.folder.text = videoList[position].folderName
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_video).centerCrop())
            .into(holder.image)

        holder.root.setOnClickListener {
            when {
                videoList[position].id == PlayerActivity.nowPlayingId -> {
                    sendIntent(position = position, ref = "NowPlaying")
                }
                isFolder -> {
                    PlayerActivity.pipStatus = 1
                    sendIntent(position = position, ref = "FolderActivity")
                }
                MainActivity.search -> {
                    PlayerActivity.pipStatus = 2
                    sendIntent(position = position, ref = "SearchedVideos")
                }
                else -> {
                    PlayerActivity.pipStatus = 3
                    sendIntent(position = position, ref = "AllVideos")
                }
            }
        }

        holder.root.setOnLongClickListener {
            val customDialog = LayoutInflater.from(context)
                .inflate(R.layout.video_more_features, holder.root, false)
            val bindingMF = VideoMoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog).create()
            dialog.show()

            bindingMF.renameBtn.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()
                val customDialogRF = LayoutInflater.from(context)
                    .inflate(R.layout.rename_field, holder.root, false)
                val bindingRF = RenameFieldBinding.bind(customDialogRF)
                val dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
                    .setCancelable(false)
                    .setPositiveButton("Rename") { self, _ ->
                        self.dismiss()
                    }
                    .setNegativeButton("Cancel") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogRF.show()
                bindingRF.renameField.text = SpannableStringBuilder.valueOf(videoList[position].title)
            }
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    private fun sendIntent(position: Int, ref: String) {
        PlayerActivity.position = position
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(searchList: ArrayList<Video>) {
        videoList = ArrayList()
        videoList.addAll(searchList)
        notifyDataSetChanged()
    }

    private fun requestPermissionR() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${context.applicationContext.packageName}")
                ContextCompat.startActivity(context, intent, null)
            }
        }
    }

}