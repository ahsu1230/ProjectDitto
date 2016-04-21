package com.nextbit.aaronhsu.projectdittoimposter;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Map;

/**
 * Created by aaronhsu on 4/19/16.
 */
public class AttributesHelper {
    private static final String TAG = "Ditto.AttributesHelper";

    private final Map<String, String> mAttrs;
    private final Context mContext;

    public AttributesHelper(Context context, Map<String, String> attributes) {
        mContext = context;
        mAttrs = attributes;
    }

    public void setup(RelativeLayout view) {

    }

    public void setup(LinearLayout view) {
        setupLayoutParams(view);
        setupOrientation(view, mAttrs.get("android:orientation"));
    }

    public void setup(ScrollView view) {
        setupLayoutParams(view);
    }

    public void setup(ImageView view) {
        if (mAttrs.containsKey("android:src")) {
            String srcVal = mAttrs.get("android:src");
            String srcPath = null;
            if (srcVal.contains("ditto_look")) {
                srcPath = TransformActivity.IMAGE_PATH_1;
            } // else include other images
            if (srcPath != null) {
                view.setImageURI(Uri.parse(srcPath));
            }
        }

        setupLayoutParams(view);
    }

    public void setup(TextView view) {
        setupLayoutParams(view);
        view.setText(mAttrs.get("android:text"));
        if (mAttrs.containsKey("android:textColor")) {
            view.setTextColor(Color.parseColor(mAttrs.get("android:textColor")));
        }
    }

    public void setup(Button view) {
        setupLayoutParams(view);
        view.setText(mAttrs.get("android:text"));
        if (mAttrs.containsKey("android:textColor")) {
            view.setTextColor(Color.parseColor(mAttrs.get("android:textColor")));
        }
    }

    public void setup(RadioButton view) {
        setupLayoutParams(view);
    }

    public void applyUniversalAttributes(View view) {
        if (mAttrs.containsKey("android:id")) {
            String idString = mAttrs.get("android:id");
            // todo (aaron) is the way to do ids?
            int id = view.generateViewId();
            view.setId(id);
        }

        if (mAttrs.containsKey("android:background")) {
            view.setBackgroundColor(Color.parseColor(mAttrs.get("android:background")));
        }

        if (mAttrs.containsKey("android:padding")) {
            setupPadding(view,
                    mAttrs.get("android:padding"),
                    mAttrs.get("android:padding"),
                    mAttrs.get("android:padding"),
                    mAttrs.get("android:padding"));
        } else {
            setupPadding(view,
                    mAttrs.get("android:paddingLeft"),
                    mAttrs.get("android:paddingRight"),
                    mAttrs.get("android:paddingTop"),
                    mAttrs.get("android:paddingBottom"));
        }
    }


    //
    // Private helper methods
    //

    private int convertDpToPx(int dp) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        return (int)((dp * displayMetrics.density) + 0.5);
    }

    private int convertPxToDp(int px) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        return (int) ((px/displayMetrics.density) + 0.5);
    }

    private String convertDpToString(int dp) {
        return dp + "dp";
    }

    private int convertDpToInt(String dp) {
        if (dp == null) {
            return 0;
        }
        return Integer.valueOf(dp.substring(0, dp.length() - 2)).intValue();
    }

    private void setupPadding(View view, String left, String top, String right, String bottom) {
        view.setPadding(
                convertDpToPx(convertDpToInt(left)),
                convertDpToPx(convertDpToInt(top)),
                convertDpToPx(convertDpToInt(right)),
                convertDpToPx(convertDpToInt(bottom)));
    }

    private void setupOrientation(LinearLayout view, String str) {
        if (str == null) {
            Log.d(TAG, "setupOrientation aborted");
            return;
        }

        if (str.equals("horizontal")) {
            view.setOrientation(LinearLayout.HORIZONTAL);
        } else if (str.equals("vertical")) {
            view.setOrientation(LinearLayout.VERTICAL);
        }
    }

    private void setupLayoutParams(LinearLayout view) {
        String widthStr = mAttrs.get("android:layout_width");
        String heightStr = mAttrs.get("android:layout_height");
        if (widthStr == null || heightStr == null) {
            Log.d(TAG, "setupLayoutParams aborted");
            return;
        }
        int width = 100;
        int height = 100;

        if (widthStr.equals("wrap_content")) {
            width = LinearLayout.LayoutParams.WRAP_CONTENT;
        } else if (widthStr.equals("match_parent") || widthStr.equals("fill_parent")) {
            width = LinearLayout.LayoutParams.MATCH_PARENT;
        }

        if (heightStr.equals("wrap_content")) {
            height = LinearLayout.LayoutParams.WRAP_CONTENT;
        } else if (heightStr.equals("match_parent") || heightStr.equals("fill_parent")) {
            height = LinearLayout.LayoutParams.MATCH_PARENT;
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        if (mAttrs.containsKey("android:layout_centerInParent")) {
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
        } else if (mAttrs.containsKey("android:layout_centerHorizontal")) {
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        } else if (mAttrs.containsKey("android:layout_centerVertical")) {
            params.addRule(RelativeLayout.CENTER_VERTICAL);
        }
        view.setLayoutParams(params);
    }

    private void setupLayoutParams(View view) {
        String widthStr = mAttrs.get("android:layout_width");
        String heightStr = mAttrs.get("android:layout_height");
        if (widthStr == null || heightStr == null) {
            Log.d(TAG, "setupLayoutParams aborted");
            return;
        }
        int width, height;

        if (widthStr.equals("wrap_content")) {
            width = TableRow.LayoutParams.WRAP_CONTENT;
        } else if (widthStr.equals("match_parent") || widthStr.equals("fill_parent")) {
            width = TableRow.LayoutParams.MATCH_PARENT;
        } else {
            width = convertDpToPx(convertDpToInt(widthStr));
        }

        if (heightStr.equals("wrap_content")) {
            height = TableRow.LayoutParams.WRAP_CONTENT;
        } else if (heightStr.equals("match_parent") || heightStr.equals("fill_parent")) {
            height = TableRow.LayoutParams.MATCH_PARENT;
        } else {
            height = convertDpToPx(convertDpToInt(heightStr));
        }

        TableRow.LayoutParams params = new TableRow.LayoutParams(width, height);
        if (mAttrs.containsKey("android:layout_gravity")) {
            String gravity = mAttrs.get("android:layout_gravity");
            if ("center_horizontal".equals(gravity)) {
                params.gravity = Gravity.CENTER_HORIZONTAL;
            } else if ("center_vertical".equals(gravity)) {
                params.gravity = Gravity.CENTER_VERTICAL;
            } else if ("center".equals(gravity)) {
                params.gravity = Gravity.CENTER;
            }
        }

        if (mAttrs.containsKey("android:layout_margin")) {
            String marginStr = mAttrs.get("android:layout_margin");
            params.setMargins(
                convertDpToPx(convertDpToInt(marginStr)),
                convertDpToPx(convertDpToInt(marginStr)),
                convertDpToPx(convertDpToInt(marginStr)),
                convertDpToPx(convertDpToInt(marginStr)));
        } else {
            int leftPx = 0, topPx = 0, rightPx = 0, bottomPx = 0;
            if (mAttrs.containsKey("android:layout_marginLeft")) {
                String marginStr = mAttrs.get("android:layout_marginLeft");
                leftPx = convertDpToPx(convertDpToInt(marginStr));
            }
            if (mAttrs.containsKey("android:layout_marginTop")) {
                String marginStr = mAttrs.get("android:layout_marginTop");
                topPx = convertDpToPx(convertDpToInt(marginStr));
            }
            if (mAttrs.containsKey("android:layout_marginRight")) {
                String marginStr = mAttrs.get("android:layout_marginRight");
                rightPx = convertDpToPx(convertDpToInt(marginStr));
            }
            if (mAttrs.containsKey("android:layout_marginLeft")) {
                String marginStr = mAttrs.get("android:layout_marginBottom");
                bottomPx = convertDpToPx(convertDpToInt(marginStr));
            }
            params.setMargins(leftPx, topPx, rightPx, bottomPx);
        }

        view.setLayoutParams(params);
    }
}
