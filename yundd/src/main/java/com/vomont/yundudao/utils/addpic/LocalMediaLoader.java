package com.vomont.yundudao.utils.addpic;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LocalMediaLoader
{
    public boolean isGif;
    
    public int index = 0;
    
    private final static String[] IMAGE_PROJECTION = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media._ID};
    
    private final static String[] VIDEO_PROJECTION = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DURATION,};
    
    private int type = FunctionConfig.TYPE_IMAGE;
    
    private FragmentActivity activity;
    
    public LocalMediaLoader(FragmentActivity activity, int type, boolean isGif)
    {
        this.activity = activity;
        this.type = type;
        this.isGif = isGif;
    }
    
    public void loadAllImage(final LocalMediaLoadListener imageLoadListener)
    {
        activity.getSupportLoaderManager().initLoader(type, null, new LoaderManager.LoaderCallbacks<Cursor>()
        {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args)
            {
                CursorLoader cursorLoader = null;
                String select[] = null;
                String condition = "";
                if (isGif)
                {
                    select = new String[] {"image/jpeg", "image/png", "image/gif"};
                    condition = MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?" + " or " + MediaStore.Images.Media.MIME_TYPE + "=?";
                }
                else
                {
                    select = new String[] {"image/jpeg", "image/png", "image/webp"};
                    condition = MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?" + " or " + MediaStore.Images.Media.MIME_TYPE + "=?";
                }
                cursorLoader = new CursorLoader(activity, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION, condition, select, IMAGE_PROJECTION[2] + " DESC");
                return cursorLoader;
            }
            
            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data)
            {
                try
                {
                    ArrayList<LocalMediaFolder> imageFolders = new ArrayList<LocalMediaFolder>();
                    LocalMediaFolder allImageFolder = new LocalMediaFolder();
                    List<LocalMedia> allImages = new ArrayList<LocalMedia>();
                    if (data != null)
                    {
                        int count = data.getCount();
                        if (count > 0)
                        {
                            data.moveToFirst();
                            do
                            {
                                String path = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                                // 如原图路径不存在或者路径存在但文件不存在,就结束当前循环
                                if (TextUtils.isEmpty(path) || !new File(path).exists())
                                {
                                    continue;
                                }
                                long dateTime = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                                int duration = (type == FunctionConfig.TYPE_VIDEO ? data.getInt(data.getColumnIndexOrThrow(VIDEO_PROJECTION[4])) : 0);
                                LocalMedia image = new LocalMedia(path, dateTime, duration, type);
                                LocalMediaFolder folder = getImageFolder(path, imageFolders);
                                folder.getImages().add(image);
                                folder.setType(type);
                                index++;
                                folder.setImageNum(folder.getImageNum() + 1);
                                // 最近相册中 只添加最新的100条
                                if (index <= 100)
                                {
                                    allImages.add(image);
                                    allImageFolder.setType(type);
                                    allImageFolder.setImageNum(allImageFolder.getImageNum() + 1);
                                }
                                
                            } while (data.moveToNext());
                            
                            if (allImages.size() > 0)
                            {
                                sortFolder(imageFolders);
                                imageFolders.add(0, allImageFolder);
                                String title = "";
                                switch (type)
                                {
                                    case FunctionConfig.TYPE_VIDEO:
                                        title = "最近视频";
                                        break;
                                    case FunctionConfig.TYPE_IMAGE:
                                        title = "最近照片";
                                        break;
                                }
                                allImageFolder.setFirstImagePath(allImages.get(0).getPath());
                                allImageFolder.setName(title);
                                allImageFolder.setType(type);
                                allImageFolder.setImages(allImages);
                            }
                            imageLoadListener.loadComplete(imageFolders);
                            data.close();
                        }
                        else
                        {
                            // 如果没有相册
                            imageLoadListener.loadComplete(imageFolders);
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onLoaderReset(Loader<Cursor> loader)
            {
            }
        });
    }
    
    private void sortFolder(List<LocalMediaFolder> imageFolders)
    {
        // 文件夹按图片数量排序
        Collections.sort(imageFolders, new Comparator<LocalMediaFolder>()
        {
            @Override
            public int compare(LocalMediaFolder lhs, LocalMediaFolder rhs)
            {
                if (lhs.getImages() == null || rhs.getImages() == null)
                {
                    return 0;
                }
                int lsize = lhs.getImageNum();
                int rsize = rhs.getImageNum();
                return lsize == rsize ? 0 : (lsize < rsize ? 1 : -1);
            }
        });
    }
    
    private LocalMediaFolder getImageFolder(String path, List<LocalMediaFolder> imageFolders)
    {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();
        
        for (LocalMediaFolder folder : imageFolders)
        {
            if (folder.getName().equals(folderFile.getName()))
            {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderFile.getName());
        newFolder.setPath(folderFile.getAbsolutePath());
        newFolder.setFirstImagePath(path);
        imageFolders.add(newFolder);
        return newFolder;
    }
    
    public interface LocalMediaLoadListener
    {
        void loadComplete(List<LocalMediaFolder> folders);
    }
}
