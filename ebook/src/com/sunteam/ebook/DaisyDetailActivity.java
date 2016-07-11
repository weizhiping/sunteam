package com.sunteam.ebook;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.sunteam.ebook.adapter.MainListAdapter.OnEnterListener;
import com.sunteam.ebook.entity.DiasyNode;
import com.sunteam.ebook.entity.FileInfo;
import com.sunteam.ebook.util.DaisyFileReaderUtils;
import com.sunteam.ebook.util.EbookConstants;
import com.sunteam.ebook.view.MainView;

/**
 * Daisy索引界面
 * 
 * @author sylar
 */
public class DaisyDetailActivity extends Activity implements OnEnterListener {
	private static final String TAG = "DaisyDetailActivity";
	private static final int[] keyCodeList = { KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 };
	private FrameLayout mFlContainer = null;
	private MainView mMainView = null;
	private ArrayList<String> mMenuList = null;
	private ArrayList<DiasyNode> diasList;
	private int catalog;// 1为txt文档，2为word文档,3为disay
	private FileInfo remberFile;
	private FileInfo fileInfo;
	private ArrayList<FileInfo> fileInfoList = null;
	private String path;
	private int seq;
	private boolean isAuto = false;			//是否自动进入阅读界面

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Intent intent = getIntent();
		String name = intent.getStringExtra("name");
		path = intent.getStringExtra("path");
		catalog = intent.getIntExtra("catalogType", 0);
		seq = intent.getIntExtra("seq", -1);
		remberFile = (FileInfo) getIntent().getSerializableExtra("file");
		fileInfo = (FileInfo) getIntent().getSerializableExtra("fileinfo");
		diasList = (ArrayList<DiasyNode>) intent.getSerializableExtra("diasys");
		fileInfoList = (ArrayList<FileInfo>) getIntent().getSerializableExtra("file_list");
		isAuto = intent.getBooleanExtra("isAuto", false);
		
		initViews(name);
		
		if( isAuto )
    	{
    		mMainView.enter(isAuto);
    	}
	}

	private void initViews(String name) {
		mMenuList = new ArrayList<String>();
		initFiles();
		mFlContainer = (FrameLayout) this.findViewById(R.id.fl_container);
		mMainView = new MainView(this, this,name, mMenuList);
		mFlContainer.removeAllViews();
		mFlContainer.addView(mMainView.getView());
//		if (null != remberFile) {
//			mMainView.setSelection(remberFile.flag);
//		}
	}
   
	// 初始化显示文件
	private void initFiles() {
		if (null != diasList) {
			for (DiasyNode f : diasList) {
				mMenuList.add(f.name);
			}
		}
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
		return mMainView.onKeyDown(keyCode, event, keyCodeList);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) 
	{
		return	mMainView.onKeyUp(keyCode, event);
	}

	@Override
	public void onEnterCompleted(int selectItem, String menu, boolean isAuto) {
		DiasyNode dias = diasList.get(selectItem);
		ArrayList<DiasyNode> diaysList = DaisyFileReaderUtils.getInstance().getChildNodeList(dias.seq);
		int size = diaysList.size();
		if( ( 0 == size ) || isAuto )
		{
			Intent intent = new Intent(this, ReadDaisyActivity.class);
			intent.putExtra("name", menu);
			intent.putExtra("path", path);
			intent.putExtra("node",  dias);
			intent.putExtra("fileinfo", fileInfo);
			intent.putExtra("file_list", fileInfoList);
			startActivityForResult(intent, EbookConstants.REQUEST_CODE);
		}
		else
		{
			Intent intent = new Intent(this, DaisyDetailActivity.class);
			intent.putExtra("name", menu);
			intent.putExtra("seq", dias.seq);
			intent.putExtra("catalogType", catalog);
			intent.putExtra("path", path);
			intent.putExtra("file", remberFile);
			intent.putExtra("fileinfo", fileInfo);
			intent.putExtra("diasys",  diaysList);
			intent.putExtra("file_list", fileInfoList);
			intent.putExtra("isAuto", isAuto);
			startActivityForResult(intent, EbookConstants.REQUEST_CODE);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		switch (requestCode) 
		{
			case EbookConstants.REQUEST_CODE:		//阅读器返回
				if( RESULT_OK == resultCode )
				{
					int next = data.getIntExtra("next", EbookConstants.TO_NEXT_PART);
					
					switch( next )
					{
						case EbookConstants.TO_NEXT_PART:	//到下一个部分
							int seq = data.getIntExtra("seq", -1);
							if( -1 == seq )
							{
								if( mMainView.isDown() )
								{
									mMainView.down();
									mMainView.enter(true);
								}
								else
								{
									Intent intent = new Intent();
									intent.putExtra("next", EbookConstants.TO_NEXT_PART);
									setResult(RESULT_OK, intent);
									finish();
								}
							}
							else
							{
								ArrayList<DiasyNode> diaysList = DaisyFileReaderUtils.getInstance().getChildNodeList(seq);
								
								if( 0 == diaysList.size() )	//当前节点是叶子节点
								{
									if( mMainView.isDown() )
									{
										mMainView.down();
										mMainView.enter(true);
									}
									else
									{
										Intent intent = new Intent();
										intent.putExtra("next", EbookConstants.TO_NEXT_PART);
										setResult(RESULT_OK, intent);
										finish();
									}
								}
								else	//如果当前节点不是叶子节点，则直接进入
								{
									Intent intent = new Intent(this, DaisyDetailActivity.class);
									intent.putExtra("name", mMainView.getCurItem());
									intent.putExtra("seq", seq);
									intent.putExtra("catalogType", catalog);
									intent.putExtra("path", path);
									intent.putExtra("file", remberFile);
									intent.putExtra("fileinfo", fileInfo);
									intent.putExtra("diasys",  diaysList);
									intent.putExtra("file_list", fileInfoList);
									intent.putExtra("isAuto", true);
									startActivityForResult(intent, EbookConstants.REQUEST_CODE);
								}
							}
							break;
						case EbookConstants.TO_NEXT_BOOK:	//到下一本书
							Intent intent = new Intent();
							intent.putExtra("next", EbookConstants.TO_NEXT_BOOK);
							setResult(RESULT_OK, intent);
							finish();
							break;
						default:
							break;
					}
				}	//阅读下一个部分
				break;
			default:
				break;
		} 	
	}	
}
