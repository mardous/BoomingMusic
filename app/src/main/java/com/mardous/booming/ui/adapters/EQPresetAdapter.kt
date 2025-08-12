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

package com.mardous.booming.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.mardous.booming.R
import com.mardous.booming.ui.IEQPresetCallback
import com.mardous.booming.core.model.equalizer.EQPreset
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class EQPresetAdapter(presets: List<EQPreset>, val callback: IEQPresetCallback) :
    RecyclerView.Adapter<EQPresetAdapter.ViewHolder>() {

    var presets: List<EQPreset> by Delegates.observable(presets) { _: KProperty<*>, _: List<EQPreset>, _: List<EQPreset> ->
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_equalizer_preset, parent, false).let { itemView ->
            ViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val preset = presets[position]

        holder.title?.text = preset.getName(holder.itemView.context)
        holder.edit?.isVisible = !preset.isCustom
        holder.delete?.isVisible = !preset.isCustom
    }

    override fun getItemCount(): Int {
        return presets.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val title: TextView? = itemView.findViewById(R.id.title)
        val edit: Button? = itemView.findViewById(R.id.edit)
        val delete: Button? = itemView.findViewById(R.id.delete)

        init {
            itemView.setOnClickListener(this)

            edit?.setOnClickListener(this)
            delete?.setOnClickListener(this)
        }

        private val preset: EQPreset
            get() = presets[bindingAdapterPosition]

        override fun onClick(view: View?) {
            when (view) {
                itemView -> callback.eqPresetSelected(preset)
                edit -> callback.editEQPreset(preset)
                delete -> callback.deleteEQPreset(preset)
            }
        }
    }
}