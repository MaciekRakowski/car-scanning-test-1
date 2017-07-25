package com.quickcarprice.activities;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.example.maciek.testapplication2.R;
import com.example.macieksquickcarprice.models.ApplicationStateSingleton;

import java.util.HashMap;


public class ActivityMainPageViewer extends FragmentActivity {

    private static int mCurrentPage = 0;
    private static Parcelable mParcelable = null; // we have only one of these views.
    private static String mVin = "";
    public static Parcelable getListViewState() {
        return mParcelable;
    }

    public static void setVin(String vin) {
        mVin = vin;
    }
    public static String getVin() {
        return mVin;
    }
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;
    private final HashMap<Integer, Fragment> mScrollableViews = new HashMap<Integer, Fragment>();
    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    public void clickOption1(View view) {
        mPager.setCurrentItem(0);
    }

    public void clickOption2(View view) {
        mPager.setCurrentItem(1);
    }

    public void clickOption3(View view) {
        mPager.setCurrentItem(2);
    }

    class ScrollInfo {
        public ScrollInfo(float xpos, int position) {
            xPos = xpos;
            Position = position;
        }
        public float xPos;
        public int Position;

        @Override
        public String toString() {
            return String.format("xPos: %f, Position: %d", xPos, Position);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {


        mCurrentPage = mPager.getCurrentItem();
        ListView listView = (ListView)this.findViewById(R.id.listview);
        mParcelable = listView.onSaveInstanceState();

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getActionBar().setTitle("Quick Car scanner");
        ApplicationStateSingleton.loadCarsFromHistoryIfNotLoaded(this);
        mScrollableViews.put(0, new EnterVin());
        mScrollableViews.put(1, new VehicleHistoryView());
        mScrollableViews.put(2, new FavoritesFragment());

        setContentView(R.layout.activity_main_page_viewer);
        final TextView textViewMainPageViewer = (TextView)this.findViewById(R.id.textViewMainPageViewer);
        final View rectangle = this.findViewById(R.id.viewRectangle);
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.mainPager);

        final float rectangleWidth = convertDpToPixel(75, getApplicationContext());
        final float paddingLeft = convertDpToPixel(16, getApplicationContext());
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            float mLeft = paddingLeft;//50;
            float mWidth = rectangleWidth;//200;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                String str = String.format("positionOffset: %f\npositionOffsetPixels: %d", positionOffset, positionOffsetPixels);
                textViewMainPageViewer.setText(str);
                float translate = mLeft + positionOffset * mWidth + position * mWidth;
                rectangle.setX(translate);
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        mPager.setCurrentItem(mCurrentPage, false);
//        if (mParcelable != null) {
//            ListView listView = (ListView)this.findViewById(R.id.listview);
//            listView.onRestoreInstanceState(mParcelable);
//        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_main_page_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ActivityMainPageViewer.this.mScrollableViews.get(new Integer(position));
        }

        @Override
        public int getCount() {
            return  ActivityMainPageViewer.this.mScrollableViews.size();
        }
    }
}
