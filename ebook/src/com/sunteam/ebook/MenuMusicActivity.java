package com.sunteam.ebook;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.sunteam.ebook.adapter.MainListAdapter.OnEnterListener;
import com.sunteam.ebook.entity.ScreenManager;
import com.sunteam.ebook.view.MainView;

/**
 * 背景音乐界面
 * 
 * @author sylar
 */
public class MenuMusicActivity extends Activity implements OnEnterListener {
	private FrameLayout mFlContainer = null;
	private MainView mMainView = null;
	private ArrayList<String> mMenuList = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ScreenManager.getScreenManager().pushActivity(this);
		initViews();
	}

	private void initViews() {
		Resources res = getResources();
		
		String[] menus = res.getStringArray(R.array.array_menu_music);
		int length = menus.length;
		mMenuList = new ArrayList<String>();
		for (int i = 0; i < length; i++) {
			mMenuList.add(menus[i]);
		}
		String title = this.getString(R.string.menu_function);
		mFlContainer = (FrameLayout) this.findViewById(R.id.fl_container);
		mMainView = new MainView(this, this, title, mMenuList);
		mFlContainer.removeAllViews();
		mFlContainer.addView(mMainView.getView());
	}
    
    @Override
    public void onPause()
    {
    	if( mMainView != null )
    	{
    		mMainView.onPause();
    	}
    	super.onPause();
    }
    
    @Override
    public void onResume()
    {
    	if( mMainView != null )
    	{
    		mMainView.onResume();
    	}
    	super.onResume();
    }
 
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		return mMainView.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) 
	{
		return	mMainView.onKeyUp(keyCode, event);
	}

	@Override
	public void onEnterCompleted(int selectItem, String menu, boolean isAuto) {
		switch (selectItem) {
		case 0:
		case 1:
			startToDetail(selectItem);
			break;
		case 2:
			Intent intent = new Intent(this,MenuNumEditActivity.class);
			intent.putExtra("edit_name", mMenuList.get(selectItem));
			intent.putExtra("edit_max", 10);
			startActivity(intent);
			break;
		}
	}
	
	private void startToDetail(int flag){
		Intent intent = new Intent(this,MenuMusicDetailActivity.class);
		intent.putExtra("music_flag", flag);
		startActivity(intent);
	}
}
