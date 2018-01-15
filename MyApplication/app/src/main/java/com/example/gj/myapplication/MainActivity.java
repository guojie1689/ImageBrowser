package com.example.gj.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {

    @BindView(R.id.img_setting)
    ImageView img_setting;

    @BindView(R.id.img_view)
    ImageView img_view;

    @BindView(R.id.img_line)
    ImageView img_line;

    @BindView(R.id.btn_prev)
    ImageView btn_prev;

    @BindView(R.id.btn_next)
    ImageView btn_next;

    @BindView(R.id.txt_file_name)
    TextView txt_file_name;

    @BindView(R.id.rll_img_line)
    RelativeLayout rll_img_line;

    private List<String> imageList = new ArrayList<>();

    private static String TAG = "MainActivity";

    private int curImageIdx = 0;
    private int screenWidth;
    private int screenHeight;

    private static final int REQUEST_CHOOSER = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        updateScreenInfo();
        initLineImage();
    }

    private int lastX;
    private int lastY;

    private void updateScreenInfo() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        screenWidth = wm.getDefaultDisplay().getWidth();
        screenHeight = wm.getDefaultDisplay().getHeight();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateScreenInfo();
        setShowImageIdx(curImageIdx);

        img_line.getLayoutParams().width = screenWidth;
        rll_img_line.getLayoutParams().width = screenWidth;

        setLayoutY(rll_img_line, 50);
    }

    public static void setLayoutY(View view, int y) {
        ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams(view.getLayoutParams());
        margin.setMargins(margin.leftMargin, y, margin.rightMargin, y + margin.height);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(margin);
        view.setLayoutParams(layoutParams);
    }

    private void initLineImage() {
        rll_img_line.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //获取Action
                int ea = event.getAction();

                Log.i("TAG", "Touch:" + ea);
                switch (ea) {
                    case MotionEvent.ACTION_DOWN:   //按下
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:  //移动
                        //移动中动态设置位置
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        int left = v.getLeft() + dx;
                        int top = v.getTop() + dy;
                        int right = v.getRight() + dx;
                        int bottom = v.getBottom() + dy;
                        if (left < 0) {
                            left = 0;
                            right = left + v.getWidth();
                        }
                        if (right > screenWidth) {
                            right = screenWidth;
                            left = right - v.getWidth();
                        }
                        if (top < 0) {
                            top = 0;
                            bottom = top + v.getHeight();
                        }
                        if (bottom > screenHeight) {
                            bottom = screenHeight;
                            top = bottom - v.getHeight();
                        }

                        setLayoutY(v, top);
                        //v.layout(left, top, right, bottom);
                        Log.i("GJ", "position：" + left + ", " + top + ", " + right + ", " + bottom);
                        //将当前的位置再次设置
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:   //脱离
                        break;
                }
                return false;
            }
        });
    }

    @OnClick(R.id.img_setting)
    void chooseImageFolder() {
        Intent getContentIntent = FileUtils.createGetContentIntent();

        Intent intent = Intent.createChooser(getContentIntent, "Select a file");
        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    private List<String> getFiles(String filePath) {
        List<String> fileList = new ArrayList<>();
        File[] files = new File(filePath).listFiles();

        if (files == null || files.length == 0) {
            return fileList;
        }

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (isImageFile(f.getPath())) {
                fileList.add(f.getPath());
            }
        }
        return fileList;
    }

    private void setShowImageIdx(int idx) {
        if (imageList == null || idx >= imageList.size()) {
            Toast.makeText(this, "未找到图片 idx[" + idx + "]", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(imageList.get(idx));

        txt_file_name.setText(curImageIdx + ":" + file.getName());
        img_view.setImageBitmap(BitmapFactory.decodeFile(imageList.get(idx)));
    }

    private static boolean isImageFile(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        if (options.outWidth == -1) {
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CHOOSER:
                if (resultCode == RESULT_OK) {

                    final Uri uri = data.getData();

                    // Get the File path from the Uri
                    String path = FileUtils.getPath(this, uri);

                    Log.d(TAG, "File Path: " + path);

                    // Alternatively, use FileUtils.getFile(Context, Uri)
                    if (path != null && FileUtils.isLocal(path)) {
                        refreshImageFolder(path);
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void refreshImageFolder(String filePathString) {
        File filePath = new File(filePathString);

        imageList = getFiles(filePath.getParentFile().getPath());

        Collections.sort(imageList, new Comparator<String>() {
            @Override
            public int compare(String filename1, String fileName2) {
                File file1 = new File(filename1);
                File file2 = new File(fileName2);

                String comFileName1 = file1.getName();
                String comFileName2 = file2.getName();

                try {
                    comFileName1 = comFileName1.substring(0, comFileName1.lastIndexOf("."));
                    comFileName2 = comFileName2.substring(0, comFileName2.lastIndexOf("."));

                    return Integer.parseInt(comFileName1) - Integer.parseInt(comFileName2);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return 0;
            }
        });

        if (imageList == null) {
            Toast.makeText(this, "未找到图片文件", Toast.LENGTH_SHORT).show();
            return;
        }

        curImageIdx = imageList.indexOf(filePathString);

        //刷新ImageView
        setShowImageIdx(curImageIdx);
    }

    @OnClick(R.id.btn_prev)
    protected void prevImage() {
        if (curImageIdx <= 0) {
            Toast.makeText(this, "已经是第一张图片", Toast.LENGTH_SHORT).show();
            return;
        }
        curImageIdx--;
        setShowImageIdx(curImageIdx);
    }

    @OnClick(R.id.btn_next)
    protected void nextImage() {
        if (imageList == null || imageList.size() == 0 || curImageIdx >= (imageList.size() - 1)) {
            Toast.makeText(this, "已经是最后一张图片", Toast.LENGTH_SHORT).show();
            return;
        }

        curImageIdx++;
        setShowImageIdx(curImageIdx);
    }

    private static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private void requestReadExtelPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                }
            } else {
                Log.d(TAG, "READ permission is granted...");
            }
        }
    }
}