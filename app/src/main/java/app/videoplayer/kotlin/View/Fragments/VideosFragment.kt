package app.videoplayer.kotlin.View.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import app.videoplayer.kotlin.Adapters.VideoAdapter
import app.videoplayer.kotlin.R
import app.videoplayer.kotlin.View.Activitys.MainActivity
import app.videoplayer.kotlin.databinding.FragmentVideosBinding
import java.lang.Exception

class VideosFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_videos, container, false)
        val binding = FragmentVideosBinding.bind(view)

        binding.videoRV.setHasFixedSize(true)
        binding.videoRV.setItemViewCacheSize(10)
        binding.videoRV.itemAnimator = DefaultItemAnimator()
        binding.videoRV.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        try {
            binding.videoRV.adapter = VideoAdapter(requireContext(), MainActivity.videoList)
        } catch (e: Exception) {
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
                    Toast.makeText(requireContext(), newText.toString(), Toast.LENGTH_SHORT).show()
                }
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }
}