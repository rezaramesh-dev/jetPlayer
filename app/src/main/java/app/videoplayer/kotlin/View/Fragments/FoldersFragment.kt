package app.videoplayer.kotlin.View.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import app.videoplayer.kotlin.Adapters.FolderAdapter
import app.videoplayer.kotlin.Adapters.VideoAdapter
import app.videoplayer.kotlin.R
import app.videoplayer.kotlin.View.Activitys.MainActivity
import app.videoplayer.kotlin.databinding.FragmentFoldersBinding
import app.videoplayer.kotlin.databinding.FragmentVideosBinding

class FoldersFragment : Fragment() {

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_folders, container, false)
        val binding = FragmentFoldersBinding.bind(view)

        binding.foldersRV.setHasFixedSize(true)
        binding.foldersRV.setItemViewCacheSize(10)
        binding.foldersRV.itemAnimator = DefaultItemAnimator()
        binding.foldersRV.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.foldersRV.adapter = FolderAdapter(requireContext(), MainActivity.folderList)
        binding.totalFolders.text = "Total Folders: ${MainActivity.folderList .size}"

        return view
    }

}