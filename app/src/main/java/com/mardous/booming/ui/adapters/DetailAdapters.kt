/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mardous.booming.databinding.ItemDetailHeaderBinding
import com.mardous.booming.databinding.ItemDetailHorizontalListBinding
import com.mardous.booming.databinding.ItemDetailSectionHeaderBinding
import com.mardous.booming.databinding.ItemDetailWikiBinding
import com.mardous.booming.extensions.resources.setMarkdownText
import com.mardous.booming.extensions.resources.show

class HeaderAdapter(
    private val onBind: (ItemDetailHeaderBinding) -> Unit
) : RecyclerView.Adapter<HeaderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDetailHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    private val stableId = View.generateViewId().toLong()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDetailHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBind(holder.binding)
    }

    override fun getItemCount(): Int = 1

    override fun getItemId(position: Int): Long = stableId
}

class SectionHeaderAdapter(
    private var title: String,
    private val onSortClick: ((View) -> Unit)? = null
) : RecyclerView.Adapter<SectionHeaderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDetailSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    private val stableId = View.generateViewId().toLong()
    private var visible = true

    init {
        setHasStableIds(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setVisible(visible: Boolean) {
        if (this.visible != visible) {
            this.visible = visible
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTitle(title: String) {
        this.title = title
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDetailSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.title.text = title
        holder.binding.sortOrder.isVisible = onSortClick != null
        holder.binding.sortOrder.setOnClickListener { onSortClick?.invoke(it) }
    }

    override fun getItemCount(): Int = if (visible) 1 else 0

    override fun getItemId(position: Int): Long = stableId
}

class WikiAdapter : RecyclerView.Adapter<WikiAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDetailWikiBinding) : RecyclerView.ViewHolder(binding.root)

    private val stableId = View.generateViewId().toLong()
    private var title: String? = null
    private var content: String? = null

    init {
        setHasStableIds(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(title: String?, content: String?) {
        this.title = title
        this.content = content
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDetailWikiBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.wikiTitle.text = title
        if (content != null) {
            holder.binding.wiki.show()
            holder.binding.wiki.setMarkdownText(content!!)
            holder.binding.wiki.setOnClickListener {
                holder.binding.wiki.maxLines = (if (holder.binding.wiki.maxLines == 4) Integer.MAX_VALUE else 4)
            }
        } else {
            holder.binding.wiki.isVisible = false
        }
    }

    override fun getItemCount(): Int = if (title != null || content != null) 1 else 0

    override fun getItemId(position: Int): Long = stableId
}

class HorizontalListAdapter(
    private var title: String,
    val innerAdapter: RecyclerView.Adapter<*>,
    private val onSortClick: ((View) -> Unit)? = null
) : RecyclerView.Adapter<HorizontalListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDetailHorizontalListBinding) : RecyclerView.ViewHolder(binding.root)

    private val stableId = View.generateViewId().toLong()
    private var visible = false

    init {
        setHasStableIds(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setVisible(visible: Boolean) {
        if (this.visible != visible) {
            this.visible = visible
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTitle(title: String) {
        this.title = title
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDetailHorizontalListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.header.title.text = title
        holder.binding.header.sortOrder.isInvisible = onSortClick == null
        holder.binding.header.sortOrder.setOnClickListener { onSortClick?.invoke(it) }
        holder.binding.recyclerView.apply {
            if (layoutManager == null) {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }
            adapter = innerAdapter
        }
    }

    override fun getItemCount(): Int = if (visible) 1 else 0

    override fun getItemId(position: Int): Long = stableId
}