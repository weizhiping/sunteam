package com.sunteam.ebook;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.sunteam.ebook.adapter.MainListAdapter.OnEnterListener;
import com.sunteam.ebook.entity.FileInfo;
import com.sunteam.ebook.entity.ScreenManager;
import com.sunteam.ebook.view.MainView;

/**
 * 功能菜单主界面
 * 
 * @author sylar
 */
public class MenuActivity extends Activity implements OnEnterListener {
	private FrameLayout mFlContainer = null;
	private MainView mMainView = null;
	private ArrayList<String> mMenuList = null;
	private int currentPage;
	private int totalPage;
	private FileInfo fileInfo;
	private String currentText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ScreenManager.getScreenManager().pushActivity(this);
		Intent intent = getIntent();
		currentPage = intent.getIntExtra("page_cur", 1);
		totalPage = intent.getIntExtra("page_count", 1);
		fileInfo = (FileInfo) intent.getSerializableExtra("file");
		currentText = intent.getStringExtra("page_text");
		initViews();
	}

	private void initViews() {
		Resources res = getResources();
		
		String[] menus = res.getStringArray(R.array.array_menu);
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
		Intent intent = new Intent();
		String title = mMenuList.get(selectItem);
		switch (selectItem) {
		
		case 0:
			intent.setClass(this, MenuMarkActivity.class);
			intent.putExtra("file", fileInfo);
			intent.putExtra("page_cur", currentPage);
			intent.putExtra("page_text", currentText);
			intent.putExtra("title", title);
			startActivity(intent);
			break;
		case 1:
			intent.putExtra("page", 1);
			intent.putExtra("result", 10);
			setResult(RESULT_OK,intent);
			finish();
			break;
		case 2:
			intent.setClass(this, MenuPageEditActivity.class);
			intent.putExtra("page_count", totalPage);
			intent.putExtra("page_cur", currentPage);
			intent.putExtra("edit_name", mMenuList.get(selectItem));
			startActivity(intent);
			break;
		case 3:
			intent.setClass(this, MenuVoiceActivity.class);
			intent.putExtra("title", title);
			startActivity(intent);
			break;
		case 4:
			intent.setClass(this, MenuMusicActivity.class);
			intent.putExtra("title", title);
			startActivity(intent);
			break;
		}
	}
}
