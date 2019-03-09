package com.simplecityapps.shuttle.ui.screens.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.simplecityapps.shuttle.R
import com.simplecityapps.shuttle.ui.common.PagerAdapter
import com.simplecityapps.shuttle.ui.screens.library.albumartists.AlbumArtistsFragment
import com.simplecityapps.shuttle.ui.screens.library.albums.AlbumsFragment
import com.simplecityapps.shuttle.ui.screens.library.songs.SongsFragment
import kotlinx.android.synthetic.main.fragment_library.*

class LibraryFragment : Fragment() {


    // Lifecycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout.setupWithViewPager(viewPager, true)

        val adapter = PagerAdapter(childFragmentManager)
        adapter.addFragment("Artists", AlbumArtistsFragment.newInstance())
        adapter.addFragment("Albums", AlbumsFragment.newInstance())
        adapter.addFragment("Songs", SongsFragment.newInstance())
        adapter.notifyDataSetChanged()

        viewPager.adapter = adapter
    }
}


