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

package com.mardous.booming.ui.screen.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentSettingsBinding
import com.mardous.booming.extensions.applyHorizontalWindowInsets
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.materialSharedAxis
import com.mardous.booming.ui.component.base.AbsMainActivityFragment

/**
 * @author Christians M. A. (mardous)
 */
class SettingsFragment : AbsMainActivityFragment(R.layout.fragment_settings), NavController.OnDestinationChangedListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var childNavController: NavController? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        with(binding.appBarLayout.toolbar) {
            setNavigationIcon(R.drawable.ic_back_24dp)
            isTitleCentered = false
            setNavigationOnClickListener {
                getOnBackPressedDispatcher().onBackPressed()
            }
        }

        materialSharedAxis(view)
        view.applyHorizontalWindowInsets()

        val navHostFragment = childFragmentManager.findFragmentById(R.id.contentFrame) as NavHostFragment
        childNavController = navHostFragment.navController.apply {
            addOnDestinationChangedListener(this@SettingsFragment)
        }
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        binding.appBarLayout.title = destination.label ?: getString(R.string.settings_title)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

    override fun onResume() {
        super.onResume()
        getOnBackPressedDispatcher().addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
        childNavController?.removeOnDestinationChangedListener(this)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (mainActivity.panelState != BottomSheetBehavior.STATE_COLLAPSED) {
                mainActivity.collapsePanel()
                return
            }
            if (childNavController?.popBackStack() == false) {
                remove()
                getOnBackPressedDispatcher().onBackPressed()
                return
            }
        }
    }
}