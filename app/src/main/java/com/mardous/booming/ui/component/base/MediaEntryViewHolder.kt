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
package com.mardous.booming.ui.component.base

import android.view.View
import android.view.View.OnLongClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mardous.booming.R
import org.koin.androidx.viewmodel.ext.android.getViewModel

open class MediaEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener, OnLongClickListener {

    val imageContainer: View? = itemView.findViewById(R.id.image_container)
    val imageGradient: View? = itemView.findViewById(R.id.image_gradient)
    val image: ImageView? = itemView.findViewById(R.id.image)
    val menu: MaterialButton? = itemView.findViewById(R.id.menu)
    val play: MaterialButton? = itemView.findViewById(R.id.play)
    val imageText: TextView? = itemView.findViewById(R.id.image_text)
    val title: TextView? = itemView.findViewById(R.id.title)
    val text: TextView? = itemView.findViewById(R.id.text)
    val time: TextView? = itemView.findViewById(R.id.time)
    val dragView: View? = itemView.findViewById(R.id.drag_view)
    val paletteColorContainer: View? = itemView.findViewById(R.id.palette_color_container)

    init {
        itemView.setOnLongClickListener(this)
        itemView.setOnClickListener(this)
    }

    protected inline fun <reified T : ViewModel> getViewModel(): T? {
        return (itemView.context as? FragmentActivity)?.getViewModel<T>()
    }

    override fun onLongClick(view: View): Boolean {
        return false
    }

    override fun onClick(view: View) {}
}
