package app.videoplayer.kotlin.View.Activitys

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import app.videoplayer.kotlin.Models.Video
import app.videoplayer.kotlin.Adapters.VideoAdapter
import app.videoplayer.kotlin.R
import app.videoplayer.kotlin.databinding.ActivityFoldersBinding
import java.io.File
import java.lang.Exception

class FoldersActivity : AppCompatActivity() {

    companion object {
        lateinit var currentFolderVideo: ArrayList<Video>
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTheme(R.style.coolPinKNav)

        val position = intent.getIntExtra("position", 0)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = MainActivity.folderList[position].folderName

        currentFolderVideo = getAllVideos(MainActivity.folderList[position].id)

        binding.videoRVFA.setHasFixedSize(true)
        binding.videoRVFA.setItemViewCacheSize(10)
        binding.videoRVFA.itemAnimator = DefaultItemAnimator()
        binding.videoRVFA.layoutManager =
            LinearLayoutManager(this@FoldersActivity, LinearLayoutManager.VERTICAL, false)
        binding.videoRVFA.adapter = VideoAdapter(
            context = this@FoldersActivity,
            videoList = currentFolderVideo,
            isFolder = true
        )

        binding.totalVideoFA.text = "Total Video s: ${currentFolderVideo.size}"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    @SuppressLint("Range", "Recycle")
    private fun getAllVideos(folderId: String): ArrayList<Video> {

        val tempList = ArrayList<Video>()
        val selection = MediaStore.Video.Media.BUCKET_ID + " like? "

        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )

        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
            selection, arrayOf(folderId), MediaStore.Video.Media.DATE_ADDED + " DESC"
        )

        if (cursor != null)
            if (cursor.moveToNext())
                do {
                    val titleC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                    val folderC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                    val sizeC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))

                    try {
                        val file = File(pathC)
                        val artUriC = Uri.fromFile(file)
                        val durationC =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                                .toLong()
                        val video = Video(
                            title = titleC, id = idC, folderName = folderC, duration = durationC,
                            size = sizeC, path = pathC, artUri = artUriC
                        )
                        if (file.exists()) tempList.add(video)

                    } catch (e: Exception) {
                        Log.i("MAIN", e.message.toString())
                    }
                } while (cursor.moveToNext())

        cursor?.close()

        return tempList
    }


}