/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catrobat.catroid.ui.cast;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.catrobat.catroid.R;

public class SlidingTabsBasicFragment extends Fragment {

	static final String LOG_TAG = "SlidingTabsBasicFragment";

	private SlidingTabLayout mSlidingTabLayout;

	private ViewPager mViewPager;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.sliding_tab_basic_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
		mViewPager.setAdapter(new SamplePagerAdapter());

		mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tab_layout);
		mSlidingTabLayout.setViewPager(mViewPager);
	}

	public SlidingTabLayout getSlidingTabLayout() { return mSlidingTabLayout; }

	class SamplePagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public boolean isViewFromObject(View view, Object o) {
			return o == view;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			String[] tabTitles = {  getResources().getString(R.string.tab_device),
									getResources().getString(R.string.tab_cast) };
			return (tabTitles[position]);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View view;
			switch (position) {
				case 0:
					view = getActivity().getLayoutInflater().inflate(R.layout.cast_pager_item_large_screen,
							container, false);
					container.addView(view);
					break;
				case 1:
					view = getActivity().getLayoutInflater().inflate(R.layout.cast_pager_item_small_screen,
							container, false);
					container.addView(view);
					break;
				default:
					Log.e(LOG_TAG, "Error. No layout file available.");
					assert(false);
					return null;
			}

			/*TextView title = (TextView) view.findViewById(R.id.item_title);
			title.setText(String.valueOf(position + 1));*/

			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

	}
}
