/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.preferences.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.databinding.PreferenceDialogNowPlayingScreenBinding
import com.mardous.booming.databinding.PreferenceNowPlayingScreenItemBinding
import com.mardous.booming.extensions.dp
import com.mardous.booming.extensions.resources.hide
import com.mardous.booming.fragments.player.PlayerColorSchemeMode
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.util.Preferences

class NowPlayingScreenPreferenceDialog : DialogFragment(), ViewPager.OnPageChangeListener,
    AdapterView.OnItemSelectedListener {

    private var _binding: PreferenceDialogNowPlayingScreenBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var viewPagerAdapter: NowPlayingScreenAdapter? = null
    private var colorSchemeAdapter: ColorSchemeAdapter? = null

    private var viewPagerPosition = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = PreferenceDialogNowPlayingScreenBinding.inflate(layoutInflater)

        viewPagerAdapter = NowPlayingScreenAdapter(requireContext())
        binding.nowPlayingScreenViewPager.adapter = viewPagerAdapter
        binding.nowPlayingScreenViewPager.addOnPageChangeListener(this)
        binding.nowPlayingScreenViewPager.pageMargin = 32.dp(resources)
        binding.nowPlayingScreenViewPager.currentItem = Preferences.nowPlayingScreen.ordinal

        colorSchemeAdapter = ColorSchemeAdapter(requireContext(), mutableListOf())
        binding.colorScheme.setAdapter(colorSchemeAdapter)
        binding.colorScheme.onItemSelectedListener = this
        updateColorScheme()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.now_playing_screen_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                Preferences.nowPlayingScreen = NowPlayingScreen.entries[viewPagerPosition]
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        viewPagerPosition = position
        updateColorScheme()
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        val currentItem = NowPlayingScreen.entries.getOrNull(viewPagerPosition)
        if (currentItem != null) {
            val selectedScheme = colorSchemeAdapter?.schemes?.getOrNull(position)
            if (selectedScheme != null) {
                Preferences.setNowPlayingColorSchemeMode(currentItem, selectedScheme)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    private fun updateColorScheme() {
        val currentItem = NowPlayingScreen.entries.getOrNull(viewPagerPosition)
        if (currentItem != null) {
            val supportedSchemes = currentItem.supportedColorSchemes
            val selectedScheme = Preferences.getNowPlayingColorSchemeMode(currentItem)
            colorSchemeAdapter?.submitList(supportedSchemes)
            binding.colorScheme.isEnabled = supportedSchemes.size > 1
            binding.colorScheme.setSelection(supportedSchemes.indexOf(selectedScheme))
        }
    }

    private class NowPlayingScreenAdapter(private val context: Context) : PagerAdapter() {

        override fun instantiateItem(collection: ViewGroup, position: Int): Any {
            val nowPlayingScreen = NowPlayingScreen.entries[position]
            val inflater = LayoutInflater.from(collection.context)

            val binding = PreferenceNowPlayingScreenItemBinding.inflate(inflater)
            collection.addView(binding.root)

            binding.image.setImageResource(nowPlayingScreen.drawableResId)
            return binding.root
        }

        override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
            collection.removeView(view as View)
        }

        override fun getCount(): Int {
            return NowPlayingScreen.entries.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return context.getString(NowPlayingScreen.entries[position].titleRes)
        }
    }

    class ColorSchemeAdapter(context: Context, schemes: List<PlayerColorSchemeMode>) :
        ArrayAdapter<PlayerColorSchemeMode>(context, android.R.layout.simple_list_item_1, schemes) {

        private val inflater = LayoutInflater.from(context)
        val schemes: List<PlayerColorSchemeMode> = schemes

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_dialog_list, parent, false)
            val titleView = view.findViewById<TextView>(R.id.title)
            val descriptionView = view.findViewById<TextView>(R.id.text)
            val iconView = view.findViewById<View>(R.id.icon_view)
            iconView?.hide()

            getItem(position)?.let {
                titleView?.setText(it.titleRes)
                descriptionView?.setText(it.descriptionRes)
            }

            return view
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            val item = getItem(position)
            view.text = item?.titleRes?.let { view.context.getString(it) } ?: ""
            return view
        }

        fun submitList(newList: List<PlayerColorSchemeMode>) {
            clear()
            addAll(newList)
            notifyDataSetChanged()
        }
    }
}