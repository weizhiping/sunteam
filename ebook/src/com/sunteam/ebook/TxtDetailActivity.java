package com.sunteam.ebook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.sunteam.common.utils.dialog.PromptListener;
import com.sunteam.ebook.adapter.MainListAdapter.OnEnterListener;
import com.sunteam.ebook.db.DatabaseManager;
import com.sunteam.ebook.entity.DiasyNode;
import com.sunteam.ebook.entity.FileInfo;
import com.sunteam.ebook.util.CallbackBundle;
import com.sunteam.ebook.util.DaisyFileReaderUtils;
import com.sunteam.ebook.util.EbookConstants;
import com.sunteam.ebook.util.FileOperateUtils;
import com.sunteam.ebook.util.PublicUtils;
import com.sunteam.ebook.util.TextFileReaderUtils;
import com.sunteam.ebook.view.MainView;
import com.sunteam.ebook.word.WordParseUtils;

/**
 * 文档列表界面 目录浏览、我的收藏、最近使用文件
 * 
 * @author sylar
 */
public class TxtDetailActivity extends Activity implements OnEnterListener {
	private static final String TAG = "TxtDetailActivity";
	private static final int[] keyCodeList = { KeyEvent.KEYCODE_MENU };
	private static final int MENU_DATA = 10;
	private FrameLayout mFlContainer = null;
	private MainView mMainView = null;
	private ArrayList<String> mMenuList = null;
	private ArrayList<FileInfo> fileInfoList = null;
	private String rootPath; // 查找文件根路径
	private DatabaseManager manager;
	private int flag;// 0为目录浏览，1为我的收藏，2为最近使用，3为目录浏览中文件
	private int flagType;
	private int storage;//0为内部存储，1为外部存储
	private int catalog;//0为txt,2为word,1为disay
	private FileInfo remberFile;//路径记忆传递
	private int position;//路径记忆位置
	private boolean isResume = true;
	private UpdateRemFileReceiver fileReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ebook_activity_main);
		initViews();
		registerReceiver();
	}

	private void initViews() {
		Intent intent = getIntent();
		String name = intent.getStringExtra("name");
		flag = intent.getIntExtra("flag", 0);
		flagType = intent.getIntExtra("flagType", 0);
		storage = intent.getIntExtra("storage", 0);
		rootPath = intent.getStringExtra("path");
		catalog = intent.getIntExtra("catalogType", 0);
		remberFile = (FileInfo) getIntent().getSerializableExtra("file");
		mMenuList = new ArrayList<String>();
		fileInfoList = new ArrayList<FileInfo>();
		manager = new DatabaseManager(this);

		switch (flag) 
		{
		case 0:
			mMenuList.add(getString(R.string.ebook_external_storage));
			mMenuList.add(getString(R.string.ebook_tf_storage));
			break;
		case 1:
		case 2:
			initDataFiles(flag,catalog);
			break;
		case 3:
			initFiles();
			break;
		default:
			initFiles();
			break;
		}
		int size = fileInfoList.size();
		for (int i = 0; i < size; i++) {
			FileInfo fileInfo = fileInfoList.get(i);
			mMenuList.add(fileInfo.name);
			if(null != remberFile && remberFile.path.contains(fileInfo.path)){
				if(remberFile.flag == flag){
					position = 0;
				}else if(0 == remberFile.flag){
					position = i;
				}
			}
		}
		mFlContainer = (FrameLayout) this.findViewById(R.id.ebook_fl_container);
		mMainView = new MainView(this, this, name, mMenuList);
		mFlContainer.removeAllViews();
		mFlContainer.addView(mMainView.getView());
		if(null != remberFile){
			if(flag == 0){
				mMainView.setSelection(storage);
			}else if(mMenuList.size() > 0){
				mMainView.setSelection(position);
			}
		}
	}
	
	private void registerReceiver(){
		fileReceiver = new UpdateRemFileReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(EbookConstants.ACTION_UPDATE_FILE);
		registerReceiver(fileReceiver, filter);
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
    	if( isResume )
    	{
	    	if( mMainView != null )
	    	{
	    		mMainView.onResume();
	    	}
    	}
    	
    	isResume = true;
    	
    	if(0 != flag && 0 == fileInfoList.size()){
    		PublicUtils.showToast(TxtDetailActivity.this, getString(R.string.ebook_no_file),new PromptListener() {
				
				@Override
				public void onComplete() {
					finish();
				}
			});
    	}
    	super.onResume();
    }
 
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		return mMainView.onKeyDown(keyCode, event, keyCodeList, mCallbackBundle);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) 
	{
		return	mMainView.onKeyUp(keyCode, event);
	}
	
	//回调
	private CallbackBundle mCallbackBundle = new CallbackBundle()
	{
		@Override
		public void callback( final Bundle bundle )
		{
			if( bundle != null )
			{
				int keyCode = bundle.getInt("keyCode");
				switch( keyCode )
				{
					case KeyEvent.KEYCODE_MENU:
						if (1 == flag || 2 == flag) {
							Intent intent = new Intent(TxtDetailActivity.this, MenuDatabaseActivity.class);
							FileInfo fileInfo = fileInfoList.get(mMainView.getSelectItem());
							intent.putExtra("file", fileInfo);
							intent.putExtra("flag", flag);
							startActivityForResult(intent, MENU_DATA);
						} else if (0 != flag) {
							insertToDb();
						}
						break;
					default:
						break;
				}
			}
		}
	};

	@Override
	public void onEnterCompleted(int selectItem, String menu, boolean isAuto) {
		if(flag == 0){
			String path;
			if(0 == selectItem){
				path = FileOperateUtils.getSDPath();
			}else{
				path = FileOperateUtils.getTFDirectory(this);
			}
			if(null != path){
				Intent intent = new Intent(this, TxtDetailActivity.class);
				intent.putExtra("path", path);
				intent.putExtra("name", mMenuList.get(selectItem));
				intent.putExtra("flag", 3);
				intent.putExtra("flagType", flagType);
				intent.putExtra("catalogType", catalog);
				intent.putExtra("storage", selectItem);
				intent.putExtra("file", remberFile);
				this.startActivity(intent);
			}else{
				String tips = this.getString(R.string.ebook_tf_does_not_exist);
				PublicUtils.showToast(this, tips);
			}
			return;
		}
		// 进入到selectItem对应的界面
		FileInfo fileInfo = fileInfoList.get(selectItem);
		fileInfo.item = selectItem;
		if(catalog != 1){
			if (fileInfo.isFolder) {
				Intent intent = new Intent(this, TxtDetailActivity.class);
				intent.putExtra("path", fileInfo.path);
				intent.putExtra("name", fileInfo.name);
				intent.putExtra("flag", 10);
				intent.putExtra("flagType", flagType);
				intent.putExtra("catalogType", catalog);
				intent.putExtra("file", remberFile);
				this.startActivity(intent);
			} else {
				String name = FileOperateUtils.getFileExtensions(fileInfo.name);
				if(name.equalsIgnoreCase(EbookConstants.BOOK_WORD)||
						 name.equalsIgnoreCase(EbookConstants.BOOK_WORDX)){
					new WordAsyncTask(isAuto).execute(fileInfo);
				}else{
				//	Log.e(TAG, "----name--:" + fileInfo.name + "---selectItem-:" + selectItem);
					showFiles(fileInfo, fileInfo.path, isAuto);
				}
			}
		}else if (fileInfo.hasDaisy){
			Intent intent = new Intent(this, TxtDetailActivity.class);
			intent.putExtra("path", fileInfo.path);
			intent.putExtra("name", fileInfo.name);
			intent.putExtra("flag", 10);
			intent.putExtra("flagType", flagType);
			intent.putExtra("catalogType", catalog);
			intent.putExtra("file", remberFile);
			this.startActivity(intent);
		}else{
			DaisyFileReaderUtils.getInstance().init(fileInfo.diasyPath);
			ArrayList<DiasyNode> diasList = DaisyFileReaderUtils.getInstance()
					.getChildNodeList(-1);
			Intent intent = new Intent(this, DaisyDetailActivity.class);;
			if (null != diasList && diasList.size() > 0) {
				intent.putExtra("diasys", diasList);
			}
			fileInfo.flag = flagType;
			intent.putExtra("name", menu);
			intent.putExtra("catalogType", fileInfo.catalog);
			intent.putExtra("path", fileInfo.path);
			intent.putExtra("file", remberFile);
			intent.putExtra("fileinfo", fileInfo);
			intent.putExtra("file_list", fileInfoList);
			intent.putExtra("isAuto", isAuto);
			startActivityForResult(intent, EbookConstants.REQUEST_CODE);
		}
	}
	
	// 显示文件内容
	private void showFiles(FileInfo fileInfo, final String fullpath, boolean isAuto) {
		fileInfo.flag = flagType;
		try {
			TextFileReaderUtils.getInstance().init(fullpath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int count = TextFileReaderUtils.getInstance().getParagraphCount(); // 得到分段信息
		fileInfo.count = count;
		
		/*
		if (0 == count) // 文件为空
		{
			// 提示一下（语音和文字）
			PublicUtils.showToast(this, getString(R.string.ebook_txt_menu_null));
		} 
		else if (1 == count) // 只有一部分
		*/
		if( count <= 1 )
		{
			
			Intent intent = new Intent(this, ReadTxtActivity.class);
			intent.putExtra("file", fileInfo);
			intent.putExtra("file_list", fileInfoList);
			intent.putExtra("isAuto", isAuto);
			startActivityForResult(intent, EbookConstants.REQUEST_CODE);
//			manager.insertBookToDb(fileInfo, 2);
		} else {
			// 根据count数量显示一个list，内容形如：第1部分 第2部分 ... 第n部分
			Intent intent = new Intent(this, TxtPartActivity.class);
			intent.putExtra("file", fileInfo);
			intent.putExtra("file_list", fileInfoList);
			intent.putExtra("count", count); // 第几部分
			intent.putExtra("isAuto", isAuto);
			startActivityForResult(intent, EbookConstants.REQUEST_CODE);
//			manager.insertBookToDb(fileInfo, 2);
		}
	}

	// 初始化显示文件
	private void initFiles() {
		if(catalog == 1){
			fileInfoList = FileOperateUtils.getDaisyInDir(catalog,rootPath);
		}else{
			ArrayList<File> filesList = null;
			if (catalog == 0) {
				filesList = FileOperateUtils.getFilesInDir(rootPath,
						EbookConstants.BOOK_TXT, EbookConstants.BOOK_TXT);
			} else if(catalog == 2){
				filesList = FileOperateUtils.getFilesInDir(rootPath,
						EbookConstants.BOOK_WORD, EbookConstants.BOOK_WORDX);
			}
			if (null != filesList) {
				FileInfo fileInfo;
				for (File f: filesList) {
					if (f.isDirectory()) {
						fileInfo = new FileInfo(f.getName(), f.getPath(), true,catalog,flagType,storage);
						fileInfoList.add(fileInfo);
					} else {
						fileInfo = new FileInfo(f.getName(), f.getPath(), false,catalog,flagType,storage);
						fileInfoList.add(fileInfo);
					}
				}
			}
		}
	}
	//添加到收藏
	private void insertToDb(){
		FileInfo fileInfo = fileInfoList.get(mMainView.getSelectItem());
		boolean hasBook = manager.insertBookToDb(fileInfo, EbookConstants.BOOK_COLLECTION);
		String tips = getResources().getString(R.string.ebook_add_fav_success);
		if(hasBook){
			tips = getResources().getString(R.string.ebook_add_fav_fail);
		}
		PublicUtils.showToast(this, tips, new PromptListener(){
			@Override
			public void onComplete() {
				if( mMainView != null ){
			    	mMainView.onResume();
			    }
			}});
	}
	
	// 初始化数据库文件
	private void initDataFiles(int flag,int catalog) {
		fileInfoList = manager.querybooks(flag,catalog);
	}
	/**
	 * word转换txt
	 * @author sylar
	 *
	 */
	private class WordAsyncTask extends AsyncTask<FileInfo, Void, String> {
		private FileInfo fileInfo = null;
		private boolean isAuto = false;
		
		public WordAsyncTask(boolean isAuto)
		{
			this.isAuto = isAuto;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			String tips = TxtDetailActivity.this.getString(R.string.ebook_word_parse_tips);
			PublicUtils.showProgress(TxtDetailActivity.this, tips); 
		}

		@Override
		protected String doInBackground(FileInfo... params) {
			fileInfo = params[0];
			String newPath = null;
			String name = FileOperateUtils.getFileExtensions(fileInfo.name);
			if(name.equalsIgnoreCase(EbookConstants.BOOK_WORD)){
				 newPath = WordParseUtils.doc2txt(fileInfo.path);
			}else if(name.equalsIgnoreCase(EbookConstants.BOOK_WORDX)){
				 newPath = WordParseUtils.docx2txt(fileInfo.path);
			}
			return	newPath;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			PublicUtils.cancelProgress();
			if( !TextUtils.isEmpty(result) )
			{
				showFiles(fileInfo, result, isAuto);
			}
			else
			{
				String tips = TxtDetailActivity.this.getString(R.string.ebook_word_parse_fail);
				PublicUtils.showToast(TxtDetailActivity.this, tips, new PromptListener() {

					@Override
					public void onComplete() {
						// TODO Auto-generated method stub
						if( mMainView != null ){
				    		mMainView.onResume();
				    	}
					}
					
				});
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(RESULT_OK == resultCode){
			switch (requestCode) { 
			case EbookConstants.REQUEST_CODE: // 阅读器返回
				//Log.e(TAG, "------read result-----:" + data);
				if( data != null )
				{
					int next = data.getIntExtra("next", -1);
					if( EbookConstants.TO_NEXT_BOOK == next )
					{
						isResume = false;
						mMainView.down(true);
						mMainView.enter(true);
						 // 阅读下一本书
					}
				}else if(2 == flag){
					initDataFiles(flag,catalog);
					int select = mMainView.getSelectItem();
					String item =  mMenuList.get(select);
					mMenuList.remove(item);
					mMenuList.add(0,item);
					mMainView.updateAdapter();
					mMainView.setSelection(0);
				}else if(1 == flag){
					initDataFiles(flag,catalog);
				}
				break;
			case MENU_DATA:
				int item = data.getIntExtra("data_item", 0);
				int select = mMainView.getSelectItem();
				
				if(0 == item){
					final boolean islast = select == (mMenuList.size() - 1)?true:false;
					FileInfo info = fileInfoList.get(select);
					fileInfoList.remove(info);
					mMenuList.remove(select);
					mMainView.updateAdapter();
					manager.deleteFile(info.path , flag);
					if(0 != mMenuList.size() && islast){
						mMainView.setSelection(0);
					}
				}else if(1 == item){
					mMenuList.clear();
					mMainView.updateAdapter();
					manager.deleteFile( null, flag);
					finish();
				}
				break;
			default:
				break;
			}
		}
	}
	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(fileReceiver);
	}
	
	private class UpdateRemFileReceiver extends BroadcastReceiver { 
	      
	    @Override  
	    public void onReceive(Context context, Intent intent) {    
	        if (intent.getAction().equals(EbookConstants.ACTION_UPDATE_FILE)) {  
	        	remberFile = manager.queryLastBook(EbookConstants.BOOK_RECENT);
	        //	 Log.e(TAG, "--------remberFile--11----------:" + remberFile.flag + "--flag-:" + flag + "--type--:" + flagType);  
	        	 flagType = remberFile.flag;
	        }  
	    } 
	}
}
