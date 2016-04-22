package com.nextbit.aaronhsu.projectdittoimposter;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by aaronhsu on 4/19/16.
 */
public class ViewInflater {
    private static final String TAG = "Ditto.ViewInflater";
    private final Context mContext;

    public ViewInflater(Context context) {
        mContext = context;
    }

    public class MyFrameLayout extends FrameLayout {
        public MyFrameLayout(Context context) {
            super(context);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            Log.d(TAG, "%%% pass touch to parent! %%%");
            return true;
        }
    }

    public View createRootView(XmlPullParser parser) {
        FrameLayout root = new MyFrameLayout(mContext);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        attachToView(parser, root, null);
        return root;
    }

    private void attachToView(XmlPullParser parser, ViewGroup parent, View existingView) {
        try {
            View newView = existingView;
            int event;
            do {
                event = parser.next();
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        // ignore, we'll continue loop
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        // ignore, we'll exit later
                        break;
                    case XmlPullParser.START_TAG:
                        if (newView == null) {
                            // newView = ***
                            newView = createNewView(parser);
                            Log.d(TAG, "###### holding on to newView..." + newView);
                        } else {
                            // we are starting a new depth
                            // we haven't added this newView yet
                            // attachToView(newView)
                            View existing = createNewView(parser);
                            Log.d(TAG, "###### move up a depth... new parent: " + newView + " existing: " + existing);
                            attachToView(parser, (ViewGroup) newView, existing);
                            Log.d(TAG, "###### add newView " + newView + " to parent " + parent);
                            parent.addView(newView);
                            newView = null;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (newView == null) {
                            // we are exiting out of current depth
                            // return
                            Log.d(TAG, "###### moving down a depth");
                            return;
                        } else {
                            // append newView to currentRoot!
                            // newView = null;
                            Log.d(TAG, "###### append newView " + newView + " to parent " + parent);
                            parent.addView(newView);
                            newView = null;
                        }
                        break;
                }
            } while (event != XmlPullParser.END_DOCUMENT);
        } catch (IOException e) {
            Log.e(TAG, "Error with xml file", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error with Parser", e);
        }
    }

    private View createNewView(XmlPullParser parser) {
        View view;
        if ("RelativeLayout".equals(parser.getName())) {
            view = new RelativeLayout(mContext);
        } else if ("LinearLayout".equals(parser.getName())) {
            view = new LinearLayout(mContext);
        } else if ("ScrollView".equals(parser.getName())) {
            view = new ScrollView(mContext);
        } else if ("ImageView".equals(parser.getName())) {
            view = new ImageView(mContext);
        } else if ("TextView".equals(parser.getName())) {
            view = new TextView(mContext);
        } else if ("Button".equals(parser.getName())) {
            view = new Button(mContext);
        } else if ("RadioButton".equals(parser.getName())) {
            view = new RadioButton(mContext);
        } else {
            // unsupported view
            return null;
        }

        setAttributes(view, parser);
        return view;
    }

    private void setAttributes(View view, XmlPullParser parser) {
        Log.d(TAG, "Setting attributes for " + parser.getName() +
                " with " + parser.getAttributeCount() + " attributes");

        //
        // Setup attributes hashmap
        //
        HashMap<String, String> attrs = new HashMap<>();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i).trim().replace("\"", "");
            String attributeVal = parser.getAttributeValue(i).trim().replace("\"","");
            attrs.put(attributeName, attributeVal);
        }
        Log.d(TAG, "double check map of attributes is correct: " + attrs.size());

        //
        // Setup specific views
        //
        AttributesHelper attributesHelper = new AttributesHelper(mContext, attrs);

        // extends ViewGroup
        if (view instanceof  RelativeLayout) {
            attributesHelper.setup((RelativeLayout) view);
        } else if (view instanceof LinearLayout) {
            attributesHelper.setup((LinearLayout) view);
        }
        // extends FrameLayout
        else if (view instanceof ScrollView) {
            attributesHelper.setup((ScrollView) view);
        }
        // extends CompoundButton
        else if (view instanceof RadioButton) {
            attributesHelper.setup((RadioButton) view);
        }
        // extends TextView
        else if (view instanceof Button) {
            attributesHelper.setup((Button) view);
        } else if (view instanceof TextView) {
            attributesHelper.setup((TextView) view);
        }
        // extends View
        else if (view instanceof ImageView) {
            attributesHelper.setup((ImageView) view);
        }

        attributesHelper.applyUniversalAttributes(view);
    }



    public void printViewGroup(ViewGroup root) {
        Log.d(TAG, "Printing ViewGroup!");
        printViewGroupHelper(root, 0);
    }

    private void printViewGroupHelper(ViewGroup root, int depth) {
        Log.d(TAG, createTabs(depth) + root.getClass().getName() + " " + root);

        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);

            String childText = null;
            if (child instanceof ViewGroup) {
                ViewGroup v = (ViewGroup) child;
                if (v.getChildCount() > 0) {
                    printViewGroupHelper(v, depth + 1);
                    continue;
                }
            } else if (!(child instanceof ImageView)) {
                childText = ((TextView)child).getText().toString();
            } else {
                // Views that don't have text - ImageView
            }
            Log.d(TAG, createTabs(depth + 1) + child.getClass().getName() + " --- " + childText);
        }
    }

    private String createTabs(int numTabs) {
        String s = "";
        for (int i=0; i < numTabs; i++) {
            s += "\t";
        }
        return s;
    }
}
