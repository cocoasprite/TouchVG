﻿//! \file ViewHelper.java
//! \brief Android绘图视图辅助类
// Copyright (c) 2012-2013, https://github.com/rhcad/touchvg

package rhcad.touchvg.view;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

import rhcad.touchvg.core.CmdObserver;
import rhcad.touchvg.core.GiContextBits;
import rhcad.touchvg.core.GiCoreView;
import rhcad.touchvg.core.MgView;
import rhcad.touchvg.view.internal.ImageCache;
import rhcad.touchvg.view.internal.ResourceUtil;
import rhcad.touchvg.view.internal.ViewUtil;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

//! Android绘图视图辅助类
/*! \ingroup GROUP_ANDROID
 */
public class ViewHelper {
    private static final String TAG = "touchvg";
    private static final int JARVERSION = 50;
    private GraphView mView;
    
    static {
        System.loadLibrary("touchvg");  // 加载绘图内核动态库，以便访问JNI
        Log.i(TAG, "TouchVG V" + getVersion());
    }
    
    //! 返回绘图包的版本号，1.0.jarver.sover
    public static String getVersion() {
        return String.format(Locale.US, "1.0.%d.%d", JARVERSION, GiCoreView.getVersion());
    }
    
    //! 指定视图的构造函数
    public ViewHelper(GraphView view) {
        mView = view;
    }
    
    //! 获取当前活动视图的默认构造函数
    public ViewHelper() {
        mView = ViewUtil.activeView;
    }
    
    //! 返回当前激活视图
    public static GraphView activeView() {
        return ViewUtil.activeView;
    }
    
    //! 得到要操作的视图
    public GraphView getView() {
        return mView;
    }
    
    //! 返回视图上下文
    public Context getContext() {
        return mView.getView().getContext();
    }
    
    //! 返回内核视图的句柄, MgView 指针
    public int cmdViewHandle() {
        final GiCoreView v = mView.coreView();
        return v != null ? v.viewAdapterHandle() : 0;
    }
    
    //! 返回内核命令视图
    public MgView cmdView() {
        return MgView.fromHandle(cmdViewHandle());
    }
    
    //! 在指定的布局中创建SurfaceView绘图视图，并记下此视图
    public ViewGroup createSurfaceView(Context context, ViewGroup layout) {
        final SFGraphView view = new SFGraphView(context);
        mView = view;
        layout.addView(view, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        createDynamicShapeView(context, layout, view);
        return layout;
    }
    
    //! 在指定的布局（建议为FrameLayout）中创建普通绘图视图，并记下此视图
    public ViewGroup createGraphView(Context context, ViewGroup layout) {
        final StdGraphView view = new StdGraphView(context);
        mView = view;
        layout.addView(view, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return layout;
    }
    
    //! 在指定的布局（建议为FrameLayout）中创建 ShapeView 绘图视图，并记下此视图
    public ViewGroup createShapeView(Context context, ViewGroup layout) {
        ShapeView view = new ShapeView(context);
        mView = view;
        layout.addView(view, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return layout;
    }
    
    //! 在指定的布局中创建放大镜视图，并记下此视图
    public ViewGroup createMagnifierView(Context context, ViewGroup layout, GraphView mainView) {
        final SFGraphView view = new SFGraphView(context, mainView != null ? mainView : mView);
        mView = view;
        layout.addView(view, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        createDynamicShapeView(context, layout, view);
        return layout;
    }
    
    private void createDynamicShapeView(Context context, ViewGroup layout, GraphView view) {
        final View dynview = view.createDynamicShapeView(context);
        if (dynview != null) {
            layout.addView(dynview, new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
    }
    
    //! 设置额外的上下文操作按钮的图像ID数组，其动作序号从40起
    public static void setExtraContextImages(Context context, int[] ids) {
        ResourceUtil.setExtraContextImages(context, ids);
    }
    
    //! 得到当前命令名称
    public String getCommand() {
        final GiCoreView v = mView.coreView();
        return v != null ? v.getCommand() : "";
    }
    
    //! 启动指定名称的命令(可用的命令名在LogCat中会打印出来，例如“registerCommand 11:lines”中的“lines”)
    public boolean setCommand(String name) {
        final GiCoreView v = mView.coreView();
        return v != null && v.setCommand(mView.viewAdapter(), name);
    }
    
    //! 启动指定名称的命令，并指定JSON串的命令初始化参数
    public boolean setCommand(String name, String params) {
        final GiCoreView v = mView.coreView();
        return v != null && v.setCommand(mView.viewAdapter(), name, params);
    }
    
    //! 返回线宽，正数表示单位为0.01毫米，零表示1像素宽，负数表示单位为像素
    public int getLineWidth() {
        return Math.round(mView.coreView().getContext(false).getLineWidth());
    }
    
    //! 设置线宽，正数表示单位为0.01毫米，零表示1像素宽，负数表示单位为像素
    public void setLineWidth(int w) {
        mView.coreView().getContext(true).setLineWidth(w, true);
        mView.coreView().setContext(GiContextBits.kContextLineWidth.swigValue());
    }
    
    //! 返回像素单位的线宽，总是为正数
    public int getStrokeWidth() {
        float w = mView.coreView().getContext(false).getLineWidth();
        return Math.round(mView.coreView().calcPenWidth(mView.viewAdapter(), w));
    }
    
    //! 设置像素单位的线宽，总是为正数
    public void setStrokeWidth(int w) {
        mView.coreView().getContext(true).setLineWidth(-Math.abs(w), true);
        mView.coreView().setContext(GiContextBits.kContextLineWidth.swigValue());
    }
    
    public static final int MAX_LINESTYLE = 5;  //!< 线型最大值, 0-5:实线,虚线,点线,点划线,双点划线,空线
    
    //! 返回线型, 0-5(MAX_LINESTYLE):实线,虚线,点线,点划线,双点划线,空线
    public int getLineStyle() {
        return mView.coreView().getContext(false).getLineStyle();
    }
    
    //! 设置线型, 0-5:实线,虚线,点线,点划线,双点划线,空线
    public void setLineStyle(int style) {
        mView.coreView().getContext(true).setLineStyle(style);
        mView.coreView().setContext(GiContextBits.kContextLineStyle.swigValue());
    }
    
    //! 返回线条颜色，忽略透明度分量，0 表示不画线条
    public int getLineColor() {
        return mView.coreView().getContext(false).getLineColor().getARGB();
    }
    
    //! 设置线条颜色，忽略透明度分量，0 表示不画线条
    public void setLineColor(int argb) {
        mView.coreView().getContext(true).setLineARGB(argb);
        mView.coreView().setContext(argb == 0 ? GiContextBits.kContextLineARGB.swigValue()
                                    : GiContextBits.kContextLineRGB.swigValue());
    }
    
    //! 返回线条透明度, 0-255
    public int getLineAlpha() {
        return mView.coreView().getContext(false).getLineColor().getA();
    }
    
    //! 设置线条透明度, 0-255
    public void setLineAlpha(int alpha) {
        mView.coreView().getContext(true).setLineAlpha(alpha);
        mView.coreView().setContext(GiContextBits.kContextLineAlpha.swigValue());
    }
    
    //! 返回填充颜色，忽略透明度分量，0 表示不填充
    public int getFillColor() {
        return mView.coreView().getContext(false).getFillColor().getARGB();
    }
    
    //! 设置填充颜色，忽略透明度分量，0 表示不填充
    public void setFillColor(int argb) {
        mView.coreView().getContext(true).setFillARGB(argb);
        mView.coreView().setContext(argb == 0 ? GiContextBits.kContextFillARGB.swigValue()
                                    : GiContextBits.kContextFillRGB.swigValue());
    }
    
    //! 返回填充透明度, 0-255
    public int getFillAlpha() {
        return mView.coreView().getContext(false).getFillColor().getA();
    }
    
    //! 设置填充透明度, 0-255
    public void setFillAlpha(int alpha) {
        mView.coreView().getContext(true).setFillAlpha(alpha);
        mView.coreView().setContext(GiContextBits.kContextFillAlpha.swigValue());
    }
    
    //! 绘图属性是否正在动态修改. 拖动时先设为true，然后改变绘图属性，完成后设为false.
    public void setContextEditing(boolean editing) {
        mView.coreView().setContextEditing(editing);
    }
    
    //! 添加测试图形
    public int addShapesForTest() {
        return mView.coreView().addShapesForTest();
    }
    
    //! 放缩显示全部内容
    public boolean zoomToExtent() {
        return mView.coreView().zoomToExtent();
    }
    
    //! 放缩显示指定范围到视图区域
    public boolean zoomToModel(float x, float y, float w, float h) {
        return mView.coreView().zoomToModel(x, y, w, h);
    }
    
    //! 得到静态图形的快照
    public Bitmap snapshot() {
        return mView.snapshot();
    }
    
    //! 保存静态图形的快照到PNG文件，自动添加后缀名.png
    public boolean savePng(String filename) {
        final Bitmap bmp = mView.snapshot();
        boolean ret = false;

        if (bmp != null) {
            synchronized (bmp) {
                try {
                    filename = addExtension(filename, ".png");
                    final FileOutputStream os = new FileOutputStream(filename);
                    ret = createFolder(filename)
                            && bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }
    
    //! 导出静态图形到SVG文件，自动添加后缀名.svg
    public boolean exportSVG(String filename) {
        return false;//mView.coreView().exportSVG(addExtension(filename, ".svg"));
    }
    
    //! 返回图形总数
    public int getShapeCount() {
        return mView.coreView().getShapeCount();
    }
    
    //! 返回选中的图形个数
    public int getSelectedCount() {
        return mView.coreView().getSelectedShapeCount();
    }
    
    //! 返回选中的图形的类型, MgShapeType
    public int getSelectedType() {
        return mView.coreView().getSelectedShapeType();
    }
    
    //! 返回当前选中的图形的ID，选中多个时只取第一个
    public int getSelectedShapeID() {
        return mView.coreView().getSelectedShapeID();
    }
    
    //! 返回图形改变次数，可用于检查是否需要保存
    public int getChangeCount() {
        return mView.coreView().getChangeCount();
    }
    
    //! 得到图形的JSON内容
    public String getContent() {
        final String str = mView.coreView().getContent();
        mView.coreView().freeContent();
        return str;
    }
    
    //! 从JSON内容中加载图形
    public boolean setContent(String content) {
        return mView.coreView().setContent(content);
    }
    
    //! 从JSON文件中加载图形，自动添加后缀名.vg
    public boolean loadFromFile(String vgfile) {
        return mView != null && mView.coreView().loadFromFile(addExtension(vgfile, ".vg"));
    }
    
    //! 从JSON文件中以只读方式加载图形，自动添加后缀名.vg
    public boolean loadFromFile(String vgfile, boolean readOnly) {
        return mView != null && mView.coreView().loadFromFile(addExtension(vgfile, ".vg"), readOnly);
    }
    
    //! 保存图形到JSON文件，自动添加后缀名.vg
    public boolean saveToFile(String vgfile) {
        boolean ret = false;
        
        vgfile = addExtension(vgfile, ".vg");
        if (mView == null) {}
        else if (getShapeCount() == 0) {
            ret = new File(vgfile).delete();
        } else {
            ret = createFolder(vgfile) && mView.coreView().saveToFile(vgfile);
        }
        return ret;
    }
    
    //! 清除所有图形
    public void clearShapes() {
        mView.coreView().clear();
    }
    
    //! 返回指定后缀名的文件名
    public static String addExtension(String filename, String ext) {
        if (!filename.endsWith(ext)) {
            filename = filename.substring(0, filename.lastIndexOf('.')) + ext;
        }
        return filename;
    }
    
    //! 创建指定的文件的上一级文件夹
    public static boolean createFolder(String filename) {
        final File file = new File(filename);
        final File pf = file.getParentFile();
        return pf.exists() || pf.mkdirs();
    }
    
    //! 在默认位置插入一个程序资源中的SVG图像(id=R.raw.name)
    public int insertSVGFromResource(String name) {
        int id = ResourceUtil.getResIDFromName(getContext(), "raw", name);
        name = ImageCache.SVG_PREFIX + name;
        final Drawable d = mView.getImageCache().addSVG(
                getContext().getResources(), id, name);
        return d == null ? 0 : mView.coreView().addImageShape(
                name, ImageCache.getWidth(d), ImageCache.getHeight(d));
    }
    
    //! 在默认位置插入一个程序资源中的SVG图像(id=R.raw.name)
    public int insertSVGFromResource(int id) {
        return insertSVGFromResource(ResourceUtil.getResName(getContext(), id));
    }
    
    //! 插入一个程序资源中的SVG图像(id=R.raw.name)，并指定图像的中心位置
    public int insertSVGFromResource(String name, int xc, int yc) {
        int id = ResourceUtil.getResIDFromName(getContext(), "raw", name);
        name = ImageCache.SVG_PREFIX + name;
        final Drawable d = mView.getImageCache().addSVG(
                getContext().getResources(), id, name);
        return d == null ? 0 : mView.coreView().addImageShape(name, xc, yc,
                ImageCache.getWidth(d), ImageCache.getHeight(d));
    }
    
    //! 插入一个程序资源中的SVG图像(id=R.raw.name)，并指定图像的中心位置
    public int insertSVGFromResource(int id, int xc, int yc) {
        return insertSVGFromResource(ResourceUtil.getResName(getContext(), id), xc, yc);
    }
    
    //! 在默认位置插入一个程序资源中的位图图像(id=R.drawable.name)
    public int insertBitmapFromResource(String name) {
        int id = ResourceUtil.getDrawableIDFromName(getContext(), name);
        name = ImageCache.BITMAP_PREFIX + name;
        final Drawable d = mView.getImageCache().addBitmap(
                getContext().getResources(), id, name);
        return d == null ? 0 : mView.coreView().addImageShape(
                name, ImageCache.getWidth(d), ImageCache.getHeight(d));
    }
    
    //! 在默认位置插入一个程序资源中的位图图像(id=R.drawable.name)
    public int insertBitmapFromResource(int id) {
        return insertBitmapFromResource(ResourceUtil.getResName(getContext(), id));
    }
    
    //! 插入一个程序资源中的位图图像(id=R.drawable.name)，并指定图像的中心位置
    public int insertBitmapFromResource(String name, int xc, int yc) {
        int id = ResourceUtil.getDrawableIDFromName(getContext(), name);
        name = ImageCache.BITMAP_PREFIX + name;
        final Drawable d = mView.getImageCache().addBitmap(
                getContext().getResources(), id, name);
        return d == null ? 0 : mView.coreView().addImageShape(name, xc, yc,
                ImageCache.getWidth(d), ImageCache.getHeight(d));
    }
    
    //! 插入一个程序资源中的位图图像(id=R.drawable.name)，并指定图像的中心位置
    public int insertBitmapFromResource(int id, int xc, int yc) {
        return insertBitmapFromResource(ResourceUtil.getResName(getContext(), id), xc, yc);
    }
    
    //! 在默认位置插入一个PNG、JPEG或SVG等文件的图像
    public int insertImageFromFile(String filename) {
        String name = filename.substring(filename.lastIndexOf('/') + 1).toLowerCase(Locale.US);
        Drawable d;
        
        if (name.endsWith(".svg")) {
            d = mView.getImageCache().addSVGFile(filename, name);
        } else {
            d = mView.getImageCache().addBitmapFile(getContext().getResources(), filename, name);
        }
        return d == null ? 0 : mView.coreView().addImageShape(name,
                ImageCache.getWidth(d), ImageCache.getHeight(d));
    }
    
    //! 设置图像文件的默认路径(可以没有末尾的分隔符)，自动加载时用
    public void setImagePath(String path) {
        mView.getImageCache().setImagePath(path);
    }
    
    //! 注册命令观察者
    public void registerCmdObserver(CmdObserver observer) {
        this.cmdView().getCmdSubject().registerObserver(observer);
    }
    
    //! 注销命令观察者
    public void unregisterCmdObserver(CmdObserver observer) {
        if (this.cmdView() != null) {
            this.cmdView().getCmdSubject().unregisterObserver(observer);
        }
    }
}