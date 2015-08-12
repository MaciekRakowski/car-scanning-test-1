package com.example.maciek.testapplication2;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.macieksquickcarprice.models.ApplicationStateSingleton;

import java.util.ArrayList;
import java.util.HashMap;


public class ActivityMainPageViewer extends FragmentActivity {

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationStateSingleton.loadCarsFromHistoryIfNotLoaded(this);
        mScrollableViews.put(0, new EnterVin());
        mScrollableViews.put(1, new VehicleHistoryView());

        setContentView(R.layout.activity_main_page_viewer);
        final TextView textViewMainPageViewer = (TextView)this.findViewById(R.id.textViewMainPageViewer);
        final View rectangle = this.findViewById(R.id.viewRectangle);
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.mainPager);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            float mLeft = 50;
            float mWidth = 200;
            
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
            if (position == 2) {
                return new EnterVin();
            }
            return ActivityMainPageViewer.this.mScrollableViews.get(new Integer(position));
        }

        @Override
        public int getCount() {
            return  3;//ActivityMainPageViewer.this.mScrollableViews.size();
        }
    }
}
