package com.com.views.helpers;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Maria on 9/20/2015.
 */
public class EmptyTextPlaceholderHelper implements TextWatcher {

    private final EditText mMainEditTextView;
    private final View mPlaceHolder;

    public EmptyTextPlaceholderHelper(EditText mainEditText, View placeHolder) {
        mMainEditTextView = mainEditText;
        mPlaceHolder = placeHolder;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String text = mMainEditTextView.getText().toString();
        mPlaceHolder.setVisibility(text.length() == 0 ? View.VISIBLE : View.GONE);
    }
}
