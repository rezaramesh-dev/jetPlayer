package app.videoplayer.kotlin.View.Fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import app.videoplayer.kotlin.Adapters.VideoAdapter
import app.videoplayer.kotlin.R
import app.videoplayer.kotlin.View.Activitys.MainActivity
import app.videoplayer.kotlin.View.Activitys.PlayerActivity
import app.videoplayer.kotlin.databinding.FragmentVideosBinding
import java.lang.Exception

class VideosFragment : Fragment() {

    private lateinit var adapter: VideoAdapter
    private lateinit var binding: FragmentVideosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_videos, container, false)
        binding = FragmentVideosBinding.bind(view)

        binding.videoRV.setHasFixedSize(true)
        binding.videoRV.setItemViewCacheSize(10)
        binding.videoRV.itemAnimator = DefaultItemAnimator()
        binding.videoRV.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        try {
            adapter = VideoAdapter(requireContext(), MainActivity.videoList)
            binding.videoRV.adapter = adapter
        } catch (e: Exception) {
        }

        binding.nowPlayingBtn.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            intent.putExtra("class", "NowPlaying")
            startActivity(intent)
        }

        binding.totalVideo.text = "Total Videos: ${MainActivity.folderList.size}"

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_view, menu)
        val searchView = menu.findItem(R.id.search)?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(p0: String?): Boolean = true

            override fun onQueryTextSubmit(newText: String?): Boolean {
                if (newText != null) {
                    MainActivity.searchList = ArrayList()
                    for (video in MainActivity.videoList) {
                        if (video.title.lowercase().contains(newText.lowercase()))
                            MainActivity.searchList.add(video)
                    }
                    MainActivity.search = true
                    adapter.updateList(searchList = MainActivity.searchList)
                }
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onResume() {
        super.onResume()
        if (PlayerActivity.position != -1) binding.nowPlayingBtn.visibility = View.VISIBLE
    }
}