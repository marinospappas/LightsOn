/*
 * Copyright (C) 2017 Google Inc. All Rights Reserved.
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
package com.example.android.wearable.wear.wearaccessibilityapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.List;
import java.util.Objects;

public class HouseRecyclerViewAdapter extends RecyclerView.Adapter<HouseRecyclerViewAdapter.Holder> {

    private static final String TAG = "HouseRecyclerViewAdapter";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private List<SmartHouse.DataSet> mItems;
    private SmartHouse h = MainActivity.h;

    public HouseRecyclerViewAdapter(Context context, List<SmartHouse.DataSet> items) {
        this.mContext = context;
        this.mItems = items;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        /* Add check for viewType here if used.
        See LongListRecyclerViewAdapter for an example. */

        return new Holder(mInflater.inflate(R.layout.app_item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        if (mItems.isEmpty()) {
            return;
        }
        final SmartHouse.DataSet item = mItems.get(position);

        holder.bind(item);
        Log.d(TAG,"in onBindViewHolder, Item: " + item.mItemName);

        // Start new activity on click of specific item.
        final int pos = position;
        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { launchActivity(mContext, item); }
                });
    }

    // Launch activity for the next level (rooms)
    public void launchActivity(Context context, SmartHouse.DataSet item) {
        Log.i(TAG,"LaunchActivity - clicked Main menu item: "+item.mItemName);
        if (Objects.equals(item.mItemName, SmartHouse.STR_ALL_OFF)) {
            Log.i(TAG,"LaunchActivity - will execute all OFF for: whole house");
            h.allOff();
        }
        else {
            Log.i(TAG,"LaunchActivity - will launch RoomActivity");
            Intent intent = new Intent(context, RoomActivity.class);
            intent.putExtra(HouseActivity.EXTRA_ROOMNAME, item.mItemName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getViewType();
    }

    static class Holder extends ViewHolder {
        TextView mTextView;
        ImageView mImageView;

        public Holder(final View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.icon_text_view);
            mImageView = itemView.findViewById(R.id.icon_image_view);
        }

        /** Bind appItem info to main screen (displays the item). */
        public void bind(SmartHouse.DataSet item) {
            mTextView.setText(item.getItemName());
            mImageView.setImageResource(item.getImageId());
        }
    }

}
