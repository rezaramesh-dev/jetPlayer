package app.videoplayer.kotlin.View.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import app.videoplayer.kotlin.Adapters.VideoAdapter
import app.videoplayer.kotlin.R
import app.videoplayer.kotlin.View.Activitys.MainActivity
import app.videoplayer.kotlin.databinding.FragmentVideosBinding
import java.lang.Exception

class VideosFragment : Fragment() {

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
        }catch (e:Exception){}

        binding.totalVideo.text = "Total Videos: ${MainActivity.folderList.size}"

        return view
    }
}