/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.tomahawk.tomahawk_android.R;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.*;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 *
 */
public abstract class TomahawkBaseAdapter extends BaseAdapter {

    protected Activity mActivity;

    protected boolean mFiltered = false;

    protected List<List<TomahawkListItem>> mListArray;
    protected List<List<TomahawkListItem>> mFilteredListArray;
    private Bitmap mPlaceHolderBitmap;

    /**
     * This interface represents an item displayed in our {@link Collection} list.
     */
    public interface TomahawkListItem {

        /** @return the corresponding name/title */
        public String getName();

        /** @return the corresponding {@link Artist} */
        public Artist getArtist();

        /** @return the corresponding {@link Album} */
        public Album getAlbum();
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return (BitmapWorkerTask) bitmapWorkerTaskReference.get();
        }
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String data;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            data = params[0];
            return BitmapFactory.decodeFile(data);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = (ImageView) imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * This {@link ViewHolder} holds the data to an entry in the grid/listView
     */
    static class ViewHolder {
        protected int viewType;
        protected ImageView imageView;
        protected TextView textFirstLine;
        protected TextView textSecondLine;
    }

    /** Add a list to the {@link TomahawkGridAdapter}.
     * @param title the title of the list, which will be displayed as a header, if the list is not empty
     * @return the index of the just added list*/
    public int addList(String title) {
        mListArray.add(new ArrayList<TomahawkListItem>());
        return mListArray.size() - 1;
    }

    /** Add an item to the list with the given index
     *  @param index the index which specifies which list the item should be added to
     *  @param item the item to add
     *  @return true if successful, otherwise false*/
    public boolean addItemToList(int index, TomahawkListItem item) {
        if (hasListWithIndex(index)) {
            mListArray.get(index).add(item);
            return true;
        }
        return false;
    }

    /** test if the list with the given index exists
     *  @param index the index of the list
     *  @return true if list exists, false otherwise*/
    public boolean hasListWithIndex(int index) {
        return (mListArray.get(index) != null);
    }

    /** Removes every element from every list there is  */
    public void clearAllLists() {
        for (int i = 0; i < mListArray.size(); i++)
            mListArray.get(i).clear();
    }

    /** @return the {@link Filter}, which allows to filter the items inside the custom {@link ListView} fed by {@link TomahawkBaseAdapter}*/
    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredListArray = (List<List<TomahawkListItem>>) results.values;
                TomahawkBaseAdapter.this.notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase();
                constraint = constraint.toString().trim();
                List<List<TomahawkListItem>> filteredResults = (List<List<TomahawkListItem>>) getFilteredResults(constraint);

                FilterResults results = new FilterResults();
                synchronized (this) {
                    results.values = filteredResults;
                }

                return results;
            }

            protected List<List<TomahawkListItem>> getFilteredResults(CharSequence constraint) {
                List<List<TomahawkListItem>> filteredResults = new ArrayList<List<TomahawkListItem>>();
                if (constraint == null || constraint.toString().length() <= 1)
                    return filteredResults;

                for (int i = 0; i < mListArray.size(); i++) {
                    filteredResults.add(new ArrayList<TomahawkListItem>());
                    for (int j = 0; j < mListArray.get(i).size(); j++) {
                        TomahawkListItem item = mListArray.get(i).get(j);
                        if (item.getName().toLowerCase().contains(constraint))
                            filteredResults.get(i).add(item);
                    }
                }
                return filteredResults;
            }
        };
    }

    /**
     * @param filtered true if the list is being filtered, else false
     */
    public void setFiltered(boolean filtered) {
        this.mFiltered = filtered;
    }

    /**
     * Load a {@link Bitmap} asynchronously
     * @param pathToBitmap the file path to the {@link Bitmap} to load
     * @param imageView the {@link ImageView}, which will be used to show the {@link Bitmap}*/
    public void loadBitmap(String pathToBitmap, ImageView imageView) {
        if (cancelPotentialWork(pathToBitmap, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            if (mPlaceHolderBitmap == null)
                mPlaceHolderBitmap = BitmapFactory.decodeResource(mActivity.getResources(),
                        R.drawable.no_album_art_placeholder);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(mActivity.getResources(), mPlaceHolderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(pathToBitmap);
        }
    }

    /**
     * Checks if another running task is already associated with the {@link ImageView}
     * @param data
     * @param imageView
     * @return */
    public static boolean cancelPotentialWork(String data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.data;
            if (bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    /**
     * Used to get the {@link BitmapWorkerTask}, which is used to asynchronously load a {@link Bitmap} into to {@link ImageView}
     * @param imageView
     * @return */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
}