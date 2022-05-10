package app.videoplayer.kotlin.Adapters

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.videoplayer.kotlin.Models.Video
import app.videoplayer.kotlin.R
import app.videoplayer.kotlin.View.Activitys.PlayerActivity
import app.videoplayer.kotlin.databinding.VideoViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class VideoAdapter(
    private val context: Context,
    private val videoList: ArrayList<Video>,
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
                isFolder -> {
                    PlayerActivity.pipStatus = 1
                    sendIntent(position = position, ref = "FolderActivity")
                }
                else -> {
                    PlayerActivity.pipStatus = 2
                    sendIntent(position = position, ref = "AllVideos")
                }
            }
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

}