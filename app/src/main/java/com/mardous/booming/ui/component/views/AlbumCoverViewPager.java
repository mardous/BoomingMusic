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

package com.mardous.booming.ui.component.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

import com.mardous.booming.util.Preferences;

/**
 * @author Christians M. A. (mardous)
 */
public class AlbumCoverViewPager extends ViewPager {

	private boolean allowSwiping;

	public AlbumCoverViewPager(@NonNull Context context) {
		this(context, null);
	}

	public AlbumCoverViewPager(@NonNull Context context, AttributeSet attrs) {
		super(context, attrs);
		setAllowSwiping(Preferences.INSTANCE.getSwipeOnCover());
	}

	public void setAllowSwiping(boolean allowSwiping) {
		this.allowSwiping = allowSwiping;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (allowSwiping) {
			return super.onTouchEvent(ev);
		}
		return true;
	}
}
