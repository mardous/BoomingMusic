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

package com.mardous.booming.ui.component.base;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mardous.booming.R;

public class MediaEntryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    @Nullable
    public final View imageContainer;
    @Nullable
    public final View imageGradient;
    @Nullable
    public final ImageView image;
    @Nullable
    public final MaterialButton menu;
    @Nullable
    public final TextView imageText;
    @Nullable
    public final TextView title;
    @Nullable
    public final TextView text;
    @Nullable
    public final TextView time;
    @Nullable
    public final View dragView;
    @Nullable
    public final View paletteColorContainer;

    public MediaEntryViewHolder(@NonNull View itemView) {
        super(itemView);
        this.imageContainer = itemView.findViewById(R.id.image_container);
        this.imageGradient = itemView.findViewById(R.id.image_gradient);
        this.image = itemView.findViewById(R.id.image);
        this.menu = itemView.findViewById(R.id.menu);
        this.imageText = itemView.findViewById(R.id.image_text);
        this.title = itemView.findViewById(R.id.title);
        this.text = itemView.findViewById(R.id.text);
        this.time = itemView.findViewById(R.id.time);
        this.dragView = itemView.findViewById(R.id.drag_view);
        this.paletteColorContainer = itemView.findViewById(R.id.palette_color_container);

        itemView.setOnLongClickListener(this);
        itemView.setOnClickListener(this);
    }

    protected final void setImageTransitionName(@NonNull String transitionName) {
        if (image != null) {
            image.setTransitionName(transitionName);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }

    @Override
    public void onClick(View view) {
    }
}
